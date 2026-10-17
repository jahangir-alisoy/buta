import { catalogApi } from '../../shared/api/endpoints';

type TrackEvent = Parameters<typeof catalogApi.track>[0][number];

export function track(event: TrackEvent) {
  void catalogApi.track([event]).catch(() => undefined);
}
