import { useCallback, useState } from 'react';
import { getApiErrorStatus, isCommonHttpErrorStatus } from '@/shared/api/api-error';

export interface RequestErrorState {
  failed: boolean;
  message: string | null;
  status: number | null;
}

export default function useRequestState<T>(createInitialData: () => T) {
  const [data, setData] = useState<T>(createInitialData);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<RequestErrorState>({
    failed: false,
    message: null,
    status: null,
  });

  const beginRequest = useCallback(() => {
    setIsLoading(true);
    setError({
      failed: false,
      message: null,
      status: null,
    });
  }, []);

  const failRequest = useCallback((errorValue: unknown, fallbackMessage: string) => {
    const status = getApiErrorStatus(errorValue);

    setError({
      failed: true,
      message: errorValue instanceof Error ? errorValue.message : fallbackMessage,
      status: isCommonHttpErrorStatus(status) ? status : null,
    });
  }, []);

  const resetError = useCallback(() => {
    setError({
      failed: false,
      message: null,
      status: null,
    });
  }, []);

  return {
    data,
    setData,
    isLoading,
    setIsLoading,
    error,
    beginRequest,
    failRequest,
    resetError,
  };
}
