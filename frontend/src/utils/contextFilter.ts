export function filterReasonLabel(reason: string | null | undefined): string {
  switch (reason) {
    case 'SCORE_TOO_LOW':
      return '分数过低'
    case 'SCORE_GAP_TOO_LARGE':
      return '与 Top1 差距过大'
    case 'EXCEED_MAX_CONTEXT_CHUNKS':
      return '超出 Context 上限'
    default:
      return reason ?? ''
  }
}
