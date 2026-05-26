const koIntegerFormatter = new Intl.NumberFormat('ko-KR');
const enIntegerFormatter = new Intl.NumberFormat('en-US');
const compactIntegerFormatter = new Intl.NumberFormat('ko-KR', { notation: 'compact', maximumFractionDigits: 1 });
const costFormatter = new Intl.NumberFormat('ko-KR', { maximumFractionDigits: 1 });
const preciseCostFormatter = new Intl.NumberFormat('ko-KR', { maximumFractionDigits: 2 });
const fixedCostFormatter = new Intl.NumberFormat('ko-KR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const roundedPercentFormatter = new Intl.NumberFormat('ko-KR', { maximumFractionDigits: 1 });

function padDatePart(value: number) {
  return String(value).padStart(2, '0');
}

export function formatDateTime(value: string) {
  if (value.trim() === '') {
    return '-';
  }

  const parsedDate = new Date(value);
  if (Number.isNaN(parsedDate.getTime())) {
    return value;
  }

  return `${parsedDate.getFullYear()}-${padDatePart(parsedDate.getMonth() + 1)}-${padDatePart(parsedDate.getDate())} ${padDatePart(parsedDate.getHours())}:${padDatePart(parsedDate.getMinutes())}:${padDatePart(parsedDate.getSeconds())}`;
}

export function formatBoardDate(value: string) {
  const parsedDate = new Date(value);
  if (Number.isNaN(parsedDate.getTime())) {
    return '-';
  }

  const year = String(parsedDate.getFullYear());
  const month = padDatePart(parsedDate.getMonth() + 1);
  const day = padDatePart(parsedDate.getDate());
  const hours = padDatePart(parsedDate.getHours());
  const minutes = padDatePart(parsedDate.getMinutes());
  return `${year}-${month}-${day} ${hours}:${minutes}`;
}

export function formatCompactBoardDate(value: string) {
  const parsedDate = new Date(value);
  if (Number.isNaN(parsedDate.getTime())) {
    return '-';
  }

  const year = String(parsedDate.getFullYear()).slice(-2);
  const month = padDatePart(parsedDate.getMonth() + 1);
  const day = padDatePart(parsedDate.getDate());
  const hours = padDatePart(parsedDate.getHours());
  const minutes = padDatePart(parsedDate.getMinutes());
  return `${year}-${month}-${day} ${hours}:${minutes}`;
}

export function formatSubmittedAt(value: string) {
  return formatDateTime(value);
}

export function formatInteger(value?: number, locale: 'ko-KR' | 'en-US' = 'ko-KR') {
  if (value == null) {
    return '-';
  }

  return locale === 'en-US' ? enIntegerFormatter.format(value) : koIntegerFormatter.format(value);
}

export function formatPercent(value: number) {
  return `${value.toFixed(1)}%`;
}

export function formatRoundedPercent(value: number) {
  return `${roundedPercentFormatter.format(Math.round(value * 10) / 10)}%`;
}

export function formatCompactInteger(value: number) {
  return compactIntegerFormatter.format(value);
}

export function formatCost(value: number) {
  return costFormatter.format(Math.round(value * 10) / 10);
}

export function formatPreciseCost(value: number) {
  return preciseCostFormatter.format(Math.round(value * 100) / 100);
}

export function formatFixedCostParts(value: number) {
  const [integerPart, fractionPart = '00'] = fixedCostFormatter.format(Math.round(value * 100) / 100).split('.');
  return {
    integerPart,
    fractionPart: fractionPart.padEnd(2, '0'),
  };
}

export function formatAlarmTime(value: string) {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return '-';
  }

  const month = padDatePart(date.getMonth() + 1);
  const day = padDatePart(date.getDate());
  const hours = padDatePart(date.getHours());
  const minutes = padDatePart(date.getMinutes());

  return `${month}-${day} ${hours}:${minutes}`;
}
