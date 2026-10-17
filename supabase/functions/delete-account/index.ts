// delete-account — permanently deletes the calling user's account and data.
//
// Runs with the service-role key, which exists only in this function's environment (never in the
// app). The caller is identified from their own JWT; nobody can delete anyone else.
//
// Order: storage objects of owned collections → database rows (delete_user_data) → auth user.
// Each step is safe to retry if a later step fails.
import { createClient } from "jsr:@supabase/supabase-js@2";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (req.method !== "POST") return json({ error: "Use POST" }, 405);

  const url = Deno.env.get("SUPABASE_URL")!;
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
  const jwt = (req.headers.get("Authorization") ?? "").replace(/^Bearer\s+/i, "");
  if (!jwt) return json({ error: "Sign in required" }, 401);

  const admin = createClient(url, serviceKey, { auth: { persistSession: false } });
  const { data: userData, error: userError } = await admin.auth.getUser(jwt);
  if (userError || !userData?.user) return json({ error: "Sign in required" }, 401);
  const userId = userData.user.id;

  // 1. Photos in collections this user owns.
  const { data: owned, error: ownedError } = await admin.from("collection").select("id").eq("owner_id", userId);
  if (ownedError) return json({ error: ownedError.message }, 500);
  for (const { id } of owned ?? []) {
    for (;;) {
      const { data: files, error } = await admin.storage.from("photos").list(id, { limit: 1000 });
      if (error) return json({ error: error.message }, 500);
      if (!files || files.length === 0) break;
      const { error: removeError } = await admin.storage.from("photos").remove(files.map((f) => `${id}/${f.name}`));
      if (removeError) return json({ error: removeError.message }, 500);
      if (files.length < 1000) break;
    }
  }

  // 2. Database rows: owned collections cascade; shared-household memberships are removed.
  const { data: deleted, error: dataError } = await admin.rpc("delete_user_data", { p_user_id: userId });
  if (dataError) return json({ error: dataError.message }, 500);

  // 3. The auth user itself.
  const { error: authError } = await admin.auth.admin.deleteUser(userId);
  if (authError) return json({ error: authError.message }, 500);

  return json({ deleted: true, ...deleted });
});

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { ...cors, "Content-Type": "application/json" } });
}
