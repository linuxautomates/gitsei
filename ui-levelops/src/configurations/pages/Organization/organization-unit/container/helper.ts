export const getIntegrationOptions = (section: any, sectionList: any[], selectworkspace: any) => {
  const alreadySelectedIds = sectionList
    ?.map((section: any) => {
      const applicationAndId: string[] = (section?.type || "")?.split("@");
      return applicationAndId?.[1];
    })
    .filter((id: any) => !!id);
  const sectionId = (section?.type || "")?.split("@")?.[1];
  const filteredIntegrations = (selectworkspace?.integrations ?? [])
    .filter((record: any) => !alreadySelectedIds.includes(record?.id) || record?.id === sectionId)
    .map((record: any) => ({
      label: record?.name,
      value: `${record?.application}@${record?.id}`
    }));
  return [...filteredIntegrations];
};
