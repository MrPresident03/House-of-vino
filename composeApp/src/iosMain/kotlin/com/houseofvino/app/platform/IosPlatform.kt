package com.houseofvino.app.platform

import com.houseofvino.core.money.MoneyFormatStyle
import com.houseofvino.core.time.IsoDate
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSBundle
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSFileManager
import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterCurrencyStyle
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSString
import platform.Foundation.NSTimeZone
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.currentLocale
import platform.Foundation.dataWithBytes
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.localTimeZone
import platform.Foundation.secondsFromGMT
import platform.Foundation.stringWithContentsOfURL
import platform.Foundation.writeToFile
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import platform.UIKit.UIActivityViewController
import platform.UIKit.popoverPresentationController
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeCommaSeparatedText
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.UniformTypeIdentifiers.UTTypeItem
import platform.UniformTypeIdentifiers.UTTypePlainText
import platform.UniformTypeIdentifiers.UTTypeTabSeparatedText
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
class IosPlatform : PlatformServices {
    override val platformName = "iOS"

    override val appVersion: String =
        (NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String) ?: "dev"

    override fun today(): IsoDate {
        val c: NSDateComponents = NSCalendar.currentCalendar.components(NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay, fromDate = NSDate())
        return IsoDate.of(c.year.toInt(), c.month.toInt(), c.day.toInt())
    }

    override fun utcOffsetMinutes(): Int = (NSTimeZone.localTimeZone.secondsFromGMT / 60).toInt()

    override fun moneyFormat(): MoneyFormatStyle {
        val f = NSNumberFormatter().apply { locale = NSLocale.currentLocale; numberStyle = NSNumberFormatterCurrencyStyle }
        val sample = f.stringFromNumber(NSNumber(double = 1.0)) ?: ""
        val symbolBefore = sample.firstOrNull()?.isDigit() == false
        val spaced = sample.any { it == ' ' || it == ' ' || it == ' ' }
        return MoneyFormatStyle(
            decimalSeparator = f.decimalSeparator?.firstOrNull() ?: '.',
            groupSeparator = f.groupingSeparator?.firstOrNull(),
            symbolBefore = symbolBefore,
            spaceBetween = spaced,
        )
    }

    override fun prefersReducedMotion(): Boolean = UIAccessibilityIsReduceMotionEnabled()

    // ---- Files -------------------------------------------------------------------------------

    private fun dir(kind: ULong, name: String): String {
        val base = NSSearchPathForDirectoriesInDomains(kind, NSUserDomainMask, true).first() as String
        val path = "$base/$name"
        NSFileManager.defaultManager.createDirectoryAtPath(path, withIntermediateDirectories = true, attributes = null, error = null)
        return path
    }

    override suspend fun shareText(fileName: String, mimeType: String, content: String): String? {
        val path = "${dir(NSCachesDirectory, "exports")}/$fileName"
        val ok = (NSString.create(string = content) as NSString).writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
        if (!ok) return "Couldn't write the export file."
        dispatch_async(dispatch_get_main_queue()) {
            val vc = UIActivityViewController(activityItems = listOf(NSURL.fileURLWithPath(path)), applicationActivities = null)
            topViewController()?.let { top ->
                vc.popoverPresentationController?.sourceView = top.view
                top.presentViewController(vc, animated = true, completion = null)
            }
        }
        return null
    }

    private var documentDelegate: DocumentDelegate? = null
    private var photoDelegate: PhotoPickerDelegate? = null

    override fun pickTextFile(kind: PickKind, onPicked: (PickedFile?) -> Unit) {
        // Sync files arrive from messaging apps with any type, so accept any file for them.
        val types = if (kind == PickKind.SYNC) listOf(UTTypeItem) else listOf(UTTypeCommaSeparatedText, UTTypeTabSeparatedText, UTTypePlainText)
        val picker = UIDocumentPickerViewController(forOpeningContentTypes = types, asCopy = true)
        val delegate = DocumentDelegate { url ->
            documentDelegate = null
            if (url == null) { onPicked(null); return@DocumentDelegate }
            val text = NSString.stringWithContentsOfURL(url, encoding = NSUTF8StringEncoding, error = null)
            onPicked(text?.let { PickedFile(url.lastPathComponent ?: "import.csv", it) })
        }
        documentDelegate = delegate
        picker.delegate = delegate
        topViewController()?.presentViewController(picker, animated = true, completion = null)
    }

    override fun pickPhoto(onPicked: (ByteArray?) -> Unit) {
        val config = PHPickerConfiguration().apply { filter = PHPickerFilter.imagesFilter; selectionLimit = 1 }
        val picker = PHPickerViewController(configuration = config)
        val delegate = PhotoPickerDelegate { data ->
            photoDelegate = null
            onPicked(data?.let { resizedJpeg(it) })
        }
        photoDelegate = delegate
        picker.delegate = delegate
        topViewController()?.presentViewController(picker, animated = true, completion = null)
    }

    private var cameraDelegate: CameraPickerDelegate? = null

    override fun cameraAvailable(): Boolean =
        UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)

    override fun takePhoto(onTaken: (ByteArray?) -> Unit) {
        if (!cameraAvailable()) return onTaken(null)
        val picker = UIImagePickerController()
        picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        val delegate = CameraPickerDelegate { data ->
            cameraDelegate = null
            onTaken(data?.let { resizedJpeg(it) })
        }
        cameraDelegate = delegate
        picker.delegate = delegate
        topViewController()?.presentViewController(picker, animated = true, completion = null) ?: onTaken(null)
    }

    override fun openUrl(url: String) {
        NSURL.URLWithString(url)?.let { UIApplication.sharedApplication.openURL(it, options = emptyMap<Any?, Any?>(), completionHandler = null) }
    }

    override fun openAppSettings() = openUrl(UIApplicationOpenSettingsURLString)

    override fun savePhoto(id: String, jpeg: ByteArray): String {
        val path = "${dir(NSApplicationSupportDirectory, "photos")}/$id.jpg"
        jpeg.toNSData().writeToFile(path, atomically = true)
        return path
    }

    override fun readPhoto(path: String): ByteArray? = NSData.dataWithContentsOfFile(path)?.toByteArray()

    override fun deletePhoto(path: String) {
        NSFileManager.defaultManager.removeItemAtPath(path, error = null)
    }

    override fun scanningAvailable(): Boolean = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) != null

    // ---- Notifications ---------------------------------------------------------------------------

    override fun setWindowReminders(enabled: Boolean, dueCount: Int) {
        // [dueCount] is deliberately unused: a count fixed at scheduling time would be wrong by delivery.
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.removePendingNotificationRequestsWithIdentifiers(listOf(REMINDER_ID))
        if (!enabled) return
        center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionBadge) { granted, _ ->
            if (!granted) return@requestAuthorizationWithOptions
            val content = UNMutableNotificationContent().apply {
                setTitle("Drinking windows")
                // iOS can't recount at delivery time, so the text stays generic rather than going stale.
                setBody("See which bottles are nearing the end of their drinking window.")
            }
            val date = NSDateComponents().apply { day = 1; hour = 10 }
            val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(date, repeats = true)
            center.addNotificationRequest(UNNotificationRequest.requestWithIdentifier(REMINDER_ID, content, trigger), withCompletionHandler = null)
        }
    }

    companion object {
        const val REMINDER_ID = "window-reminder"
    }
}

@Suppress("DEPRECATION")
internal fun topViewController(): UIViewController? {
    var vc = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (vc?.presentedViewController != null) vc = vc.presentedViewController
    return vc
}

private class DocumentDelegate(private val onUrl: (NSURL?) -> Unit) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        onUrl(didPickDocumentsAtURLs.firstOrNull() as? NSURL)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) = onUrl(null)
}

private class PhotoPickerDelegate(private val onData: (NSData?) -> Unit) : NSObject(), PHPickerViewControllerDelegateProtocol {
    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val result = didFinishPicking.firstOrNull() as? PHPickerResult ?: run { onData(null); return }
        result.itemProvider.loadDataRepresentationForTypeIdentifier(UTTypeImage.identifier) { data, _ ->
            dispatch_async(dispatch_get_main_queue()) { onData(data) }
        }
    }
}

private class CameraPickerDelegate(private val onData: (NSData?) -> Unit) : NSObject(),
    UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        onData(image?.let { UIImageJPEGRepresentation(it, 0.9) })
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onData(null)
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun resizedJpeg(data: NSData, maxEdge: Double = 2048.0): ByteArray? {
    val image = UIImage(data = data)
    val (w, h) = image.size.useContents { width to height }
    if (w <= 0.0 || h <= 0.0) return null
    val scale = minOf(1.0, maxEdge / maxOf(w, h))
    val tw = w * scale
    val th = h * scale
    // Drawing applies the photo's EXIF orientation, so the JPEG written out is upright.
    val renderer = UIGraphicsImageRenderer(size = CGSizeMake(tw, th))
    val resized = renderer.imageWithActions { _ -> image.drawInRect(CGRectMake(0.0, 0.0, tw, th)) }
    return UIImageJPEGRepresentation(resized, 0.85)?.toByteArray()
}

@OptIn(ExperimentalForeignApi::class)
internal fun ByteArray.toNSData(): NSData =
    if (isEmpty()) NSData() else usePinned { NSData.dataWithBytes(it.addressOf(0), size.toULong()) }
