# Incident Response and Breach Plan

A solo-operated product still needs a plan written before it is needed. This is the plan; it is engineering guidance, not legal advice, and the notification steps should be confirmed with a qualified person before beta (see `data-privacy-retention.md`).

## What counts

- **Security incident:** anyone who should not have access to a person's data got it, or could have (leaked database credentials, a stolen session, an exposed backup, a bug that shows one user another's data, a compromised provider account).
- **Availability incident:** the service is down or data is lost or wrong (the restore plan in `08-devops/backup-and-restore.md` covers recovery).
- **Cost incident:** AI spend or traffic is running away (`ALERT_WEBHOOK_URL` alerts; the `AI_ENABLED` kill switch).

When unsure, treat it as a security incident until shown otherwise.

## First hour: stop the bleeding

Do these in order; each is a setting or a command, none needs a deploy.

1. **Write down the time you noticed** and keep a running note of everything you do (what, when, why). You will need it for the report.
2. **Cut off the likely path:**
   - Credentials suspected: rotate them now - the Neon database passwords (both roles: update `DATABASE_PASSWORD` and `FLYWAY_DATABASE_PASSWORD` in Render), the Mistral key (`MISTRAL_API_KEY`), the alert webhook URL. The Google client id is public and needs no rotation.
   - A stolen or abused session, or unknown cause: end every session so everyone must sign in again: `DELETE FROM sessions;` through a privileged connection.
   - A bug exposing data: switch the feature off (`AI_ENABLED=false` for anything AI) or suspend the Render service, which takes the whole API offline. Taking the site down is acceptable; leaking more data is not.
3. **Keep evidence.** Download the Render logs and note Neon's query history before they age out. Do not delete anything, and do not restore over the database, until you understand what happened.

## Investigate

Answer, from logs, the database and the code history:

- What data could be reached, and whose (all users, one user, only metadata)?
- Since when, and until when (the first and last time the weakness existed)?
- Was it actually accessed, or only exposed?
- How did it happen, and is that route closed now?

Row-level security means a single-user bug usually exposes one person's rows, not the table; confirm rather than assume.

## Tell people

If personal data was exposed, the Digital Personal Data Protection Act requires telling the affected people and the Data Protection Board of India. The exact deadlines and form are set by the Rules under the Act; **confirm the current requirements with a qualified person** and write them here: ______.

A message to affected users says, plainly: what happened and when, what data of theirs was involved, what has been done about it, what they should do (change their password, watch for scam messages quoting transactions), and how to reach you. It does not minimise or blame. Send it from an address they can reply to.

If a provider (Neon, Render, Vercel, Google, Mistral) caused it, follow their incident process too and keep their written account.

## Recover and learn

1. Close the root cause, add a test that fails without the fix (a rule of this project: a bug found in the wild gets a regression test), and deploy through `staging` first unless the fix itself is urgent.
2. Restore service, then re-enable features one at a time.
3. Within a week, write a short post-incident note in `DECISIONS.md`: timeline, cause, impact, what changed. Update this plan if it failed you.

## Contacts to fill in before beta

| Who | How |
|---|---|
| Owner / operator | Deepanshu.dev.ai098@gmail.com |
| Someone qualified to advise on notification | ______ |
| Neon support | console.neon.tech, project support |
| Render support | dashboard.render.com, service support |
| Vercel support | vercel.com/help |

## Drills

Read this plan end to end and walk the first hour once on a staging copy before beta: end all sessions, rotate a database password in Render and confirm the service reconnects, switch `AI_ENABLED` off and on. Note the date here: ______.
