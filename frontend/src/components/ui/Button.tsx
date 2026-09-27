import type { ButtonHTMLAttributes } from 'react'

type Variant = 'primary' | 'secondary' | 'danger'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
  block?: boolean
  small?: boolean
}

export function Button({ variant = 'primary', block, small, className, ...rest }: ButtonProps) {
  const classes = ['btn', `btn-${variant}`, block ? 'btn-block' : '', small ? 'btn-sm' : '', className ?? '']
    .filter(Boolean)
    .join(' ')
  return <button className={classes} {...rest} />
}
