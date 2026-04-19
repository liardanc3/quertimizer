import type { DbmsOption } from '../types/domain';

export const mockDbmsOptions: DbmsOption[] = [
  { id: 'postgresql', label: 'PostgreSQL' },
  { id: 'oracle', label: 'Oracle', disabled: true },
];
