/** checks whether collection is present in already received records */
export const isOUPresentInRecords = (records: any[], orgUnitId: string) => {
  return !!(records ?? []).find(orgUnitConfig => orgUnitConfig?.id === orgUnitId);
};
