import { Link } from 'react-router-dom'
import { PRIVACY_CONTACT_EMAIL, PRIVACY_LAST_UPDATED } from '../lib/legal'

/** Public: readable without an account, linked from the sign-in pages and from Settings. */
export function PrivacyPage() {
  return (
    <div className="legal-shell">
      <article className="legal-page">
        <Link to="/" className="legal-back">
          &larr; Back to Spendwise
        </Link>
        <h1>Privacy policy</h1>
        <p className="legal-meta">Last updated {PRIVACY_LAST_UPDATED}</p>
        <div className="legal-draft">
          This is a working draft written to describe exactly what the product does today. It has not yet been reviewed by a
          lawyer, and will be before Spendwise is opened to the public.
        </div>

        <h2>What Spendwise is</h2>
        <p>
          Spendwise is a personal-finance tool: you add or import your transactions and it shows where your money goes. It is run
          by an individual operator in India. In the language of India&rsquo;s Digital Personal Data Protection Act, you are the
          data principal and the operator is the data fiduciary.
        </p>

        <h2>What we hold</h2>
        <ul>
          <li>
            <strong>Your account:</strong> your name and email address, and either a password (stored only as a salted one-way
            hash, never the password itself) or, if you sign in with Google, Google&rsquo;s identifier for your account.
          </li>
          <li>
            <strong>Your finances:</strong> what you enter or import - accounts, transactions, categories, merchants, budgets,
            goals, recurring payments and rules, and the insights generated for you.
          </li>
          <li>
            <strong>Statements you upload:</strong> the PDF is read to extract its transactions. The file itself is deleted as
            soon as you confirm the import. A statement you upload but never confirm keeps its file until you delete your
            account (there is not yet a way to remove a single statement). The extracted rows stay in your account until you
            delete them.
          </li>
          <li>
            <strong>Sign-in sessions:</strong> for each device you are signed in on, the time and the browser and device type
            your browser reports, so you can see and end your sessions.
          </li>
          <li>
            <strong>AI use counts:</strong> how many AI requests you have made each day, so daily limits can be applied. The
            questions themselves are not stored (the assistant keeps no chat history on our side); insights written for you are
            kept with your data.
          </li>
          <li>
            <strong>Technical records:</strong> short-lived security and operating logs kept by our hosting providers, and counters
            of requests held in memory for a few minutes (including your IP address) to limit abuse and guessing of passwords.
            They are kept free of your transactions, passwords and statement contents.
          </li>
        </ul>
        <p>We do not sell your data, show advertising, or run analytics or advertising trackers.</p>

        <h2>Cookies</h2>
        <p>
          Two cookies, both needed for the site to work: one keeps you signed in (not readable by scripts), and one protects
          your requests from forgery. There are no advertising or tracking cookies. If you choose &ldquo;Continue with
          Google&rdquo;, Google&rsquo;s own sign-in button loads from Google and is covered by Google&rsquo;s privacy policy.
        </p>

        <h2>AI features</h2>
        <p>
          The assistant, category suggestions and monthly insights use an AI model provided by Mistral AI. When you use them, we
          send only what the answer needs: your question, figures worked out by our own code, and the names and descriptions of
          the transactions involved - with long numbers (phone, card, account and reference numbers) and the bank part of UPI
          ids removed first. We do not send the model your password, your email address or your uploaded files, and it has no way to
          read other people&rsquo;s data. Mistral processes this under its own terms. The AI features can be switched off by the
          operator at any time and the rest of the product works without them.
        </p>

        <h2>Who else handles your data</h2>
        <ul>
          <li>Render - runs the server.</li>
          <li>Neon - hosts the database.</li>
          <li>Vercel - serves the website.</li>
          <li>Google - only if you use Google sign-in.</li>
          <li>Mistral AI - only if you use the AI features, as described above.</li>
        </ul>
        <p>
          These providers may store data outside India. We use each only to run the service, not for their own purposes.
        </p>

        <h2>Keeping it safe</h2>
        <p>
          Connections are encrypted. Each person&rsquo;s data is separated at the database level, so a bug in one screen cannot
          show you someone else&rsquo;s records. Sign-in attempts and uploads are rate limited, and repeated wrong passwords
          lock further attempts for a while. If something goes wrong that affects your data, we will tell you and the authorities
          as the law requires, and say what happened and what to do.
        </p>

        <h2>How long we keep it</h2>
        <p>
          Your data stays until you delete it. Deleting your account (Settings, then Delete account) erases your account and
          everything in it straight away - transactions, accounts, budgets, goals, statements and uploaded files. Backups made
          before a deletion age out and are not restored without re-applying deletions.
        </p>

        <h2>Your rights</h2>
        <ul>
          <li>
            <strong>See and take your data:</strong> Settings, then Download my data, gives you everything in one file.
          </li>
          <li>
            <strong>Correct it:</strong> you can edit the transactions, accounts, budgets and goals you entered in the app. To
            change your name or email, contact us.
          </li>
          <li>
            <strong>Erase it:</strong> Settings, then Delete account.
          </li>
          <li>
            <strong>Withdraw consent:</strong> stop using the AI features at any time, or delete your account.
          </li>
          <li>
            <strong>Complain:</strong> to us first, using the contact below; if you are not satisfied, to the Data Protection
            Board of India.
          </li>
        </ul>

        <h2>Who can use Spendwise</h2>
        <p>Spendwise is for adults. It is not meant for anyone under 18.</p>

        <h2>Contact</h2>
        <p>
          {PRIVACY_CONTACT_EMAIL ? (
            <>
              Questions, corrections or complaints: <a href={`mailto:${PRIVACY_CONTACT_EMAIL}`}>{PRIVACY_CONTACT_EMAIL}</a>
            </>
          ) : (
            'The contact address for privacy questions and complaints will be added here before the public beta.'
          )}
        </p>

        <h2>Changes</h2>
        <p>If this policy changes in a way that matters, we will say so in the app before it takes effect.</p>
      </article>
    </div>
  )
}
