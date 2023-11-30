export function checkTemplateNameExists(prevName: string, data: any): boolean {
  return !!data?.filter((item: any) => item?.name?.toLowerCase() === prevName?.toLowerCase())?.length || false;
}
