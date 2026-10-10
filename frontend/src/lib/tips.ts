/**
 * Short money tips and facts about how Spendwise works, shown on the dashboard. All written for this app - none is a quote from a
 * named person, so nothing can be misattributed - and each is either a widely used rule of thumb or a true statement about the product.
 * They are general information, not financial advice.
 */
export interface Tip {
  kind: 'tip' | 'app'
  text: string
}

export const TIPS: Tip[] = [
  { kind: 'tip', text: 'Pay yourself first: move a fixed amount to savings the day your salary arrives, before you spend any of it.' },
  { kind: 'tip', text: 'A common rule of thumb is 50/30/20: about half your income for needs, 30% for wants, and 20% for savings or paying off debt.' },
  { kind: 'tip', text: 'An emergency fund covering three to six months of essential expenses is a widely used safety net.' },
  { kind: 'tip', text: 'Small recurring payments add up: a ₹499 monthly subscription costs ₹5,988 over a year.' },
  { kind: 'tip', text: 'Look through your subscriptions every few months. Cancelling one you no longer use is the easiest saving there is.' },
  { kind: 'tip', text: 'Check statements for fees and charges. Each one is small, which is why they are easy to miss.' },
  { kind: 'tip', text: 'Budget the categories where you tend to overspend rather than everything at once. It is easier to keep.' },
  { kind: 'tip', text: 'Paying a credit card bill in full by its due date avoids interest charges on your purchases.' },
  { kind: 'tip', text: 'Record a payment the day you make it. It takes seconds and keeps your picture accurate.' },
  { kind: 'tip', text: 'Compare a month with the same month last year, not only with the month before: spending has seasons, like festivals, travel and insurance.' },
  { kind: 'tip', text: 'Treat a savings goal like a bill: decide the monthly amount and the date, and put it on a schedule.' },
  { kind: 'tip', text: 'Cash is hard to track. Note what you spent it on, or it simply disappears from your picture.' },
  { kind: 'tip', text: 'A budget is a plan for your money, not a punishment. Adjust it when your life changes.' },
  { kind: 'tip', text: 'When a bill is higher than usual, look at the earlier months before reacting; one month alone can mislead.' },
  { kind: 'app', text: 'Refunds count against your spending in Spendwise, so your totals stay honest.' },
  { kind: 'app', text: 'Every figure in an AI insight is computed by Spendwise and checked before you see it. The AI only writes the words.' },
  { kind: 'app', text: 'Nothing from a statement counts until you confirm it. You can edit or skip any row first.' },
  { kind: 'app', text: 'Upload the same statement twice and Spendwise notices, so nothing is counted double.' },
  { kind: 'app', text: 'The database itself restricts every query to your own account, as well as the application.' },
  { kind: 'app', text: 'Tap the chat button and ask about any payment, for example “what did I pay Sharma in March?”.' },
  { kind: 'app', text: 'A budget can repeat every month, or cover a trip or festival with fixed dates.' },
  { kind: 'app', text: 'A goal shows how much to set aside each month to reach it by your date.' },
  { kind: 'app', text: 'Recurring payments are found automatically once something repeats at a regular interval, usually three times.' },
  { kind: 'app', text: 'When AI suggests a category for an imported row, it sees only the cleaned description: no amounts and no account details.' },
  { kind: 'app', text: 'If the AI is switched off or unavailable, your insight is still written, from fixed sentences built on the same figures.' },
]

function dayOfYear(date: Date): number {
  const start = new Date(date.getFullYear(), 0, 0)
  return Math.floor((date.getTime() - start.getTime()) / 86_400_000)
}

/** The tip for a day: it changes daily, and everyone sees the same one that day. offset steps to the following tips. */
export function tipForDate(date: Date, offset = 0): Tip {
  const index = (dayOfYear(date) + date.getFullYear() * 7 + offset) % TIPS.length
  return TIPS[((index % TIPS.length) + TIPS.length) % TIPS.length]
}
