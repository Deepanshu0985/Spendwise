interface ComingSoonPageProps {
  title: string
  phase: string
}

export function ComingSoonPage({ title, phase }: ComingSoonPageProps) {
  return (
    <section>
      <h1>{title}</h1>
      <p>Coming in {phase}. See docs/09-project/phase-plan.md.</p>
    </section>
  )
}
