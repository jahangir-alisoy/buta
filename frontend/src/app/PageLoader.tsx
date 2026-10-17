import { Skeleton } from '../design-system/components/States';

export function PageLoader() {
  return (
    <div className="container page stack" aria-busy="true">
      <Skeleton height={36} width="45%" />
      <Skeleton height={18} width="70%" />
      <Skeleton height={220} radius={24} />
    </div>
  );
}
