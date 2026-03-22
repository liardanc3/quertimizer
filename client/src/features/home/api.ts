import { http } from '../../shared/api/http';

export function getTestMessage() {
  return http.get<string>('/test');
}