import type { MockResult } from '../types/domain';

export const mockSuccessResult: MockResult = {
  status: 'success',
  message: '정답입니다. 성능 조건도 통과했습니다.',
  executionTimeMs: 18.4,
  scanRows: 11000,
  cost: 52,
  indexUsed: true,
  fullScan: false,
  rows: [
    { columns: ['2025-11', 'A12', '812000'] },
    { columns: ['2025-11', 'B02', '743500'] },
    { columns: ['2025-11', 'C31', '701200'] },
  ],
};

export const mockFailResult: MockResult = {
  status: 'fail',
  message: '정확도는 통과했지만, 성능 기준(30ms 이하)을 초과했습니다.',
  executionTimeMs: 76.9,
  scanRows: 58200,
  cost: 410,
  indexUsed: false,
  fullScan: true,
  rows: [
    { columns: ['2025-11', 'A12', '812000'] },
    { columns: ['2025-11', 'B02', '743500'] },
  ],
};
