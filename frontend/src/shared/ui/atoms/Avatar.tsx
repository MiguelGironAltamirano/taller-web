import { cn } from '../../lib/cn'
import { initials } from '../../lib/initials'

type AvatarProps = {
  name: string
  size?: 'xs' | 'sm' | 'md'
  tone?: 'primary' | 'soft' | 'neutral'
  className?: string
}

const SIZE = { xs: 'size-6 text-[10px]', sm: 'size-7 text-xs', md: 'size-8 text-xs' }
const TONE = {
  primary: 'bg-primary text-on-primary',
  soft: 'bg-primary-soft text-primary-fg',
  neutral: 'border border-line bg-surface2 text-fg',
}

export function Avatar({ name, size = 'md', tone = 'neutral', className }: AvatarProps) {
  return (
    <span
      aria-hidden="true"
      className={cn('grid shrink-0 place-items-center rounded-full font-semibold', SIZE[size], TONE[tone], className)}
    >
      {initials(name)}
    </span>
  )
}
