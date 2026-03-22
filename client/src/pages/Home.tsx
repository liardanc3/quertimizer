import { useEffect, useState } from 'react';
import { getTestMessage } from '../features/home/api';

export default function App() {
  const [message, setMessage] = useState('불러오는 중...');
  const [error, setError] = useState('');

  useEffect(() => {
    getTestMessage()
      .then((data) => {
        setMessage(data.result);
      })
      .catch((err) => {
        setError(err.message);
      });
  }, []);

  return (
    <div style={{ padding: '24px' }}>
      <h1>홈페이지</h1>
      {error ? <p>에러: {error}</p> : <p>{message}</p>}
    </div>
  );
}