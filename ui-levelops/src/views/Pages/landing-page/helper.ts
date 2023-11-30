export const buildLink = (ouMappings: Record<string, any>, selectedOu: string, ouGroupId: Array<string>) => {
  if (ouMappings?.[selectedOu]) {
    const obj = ouMappings?.[selectedOu];
    ouGroupId.unshift(obj?.ou_id);
    if (ouMappings?.[obj?.parent_ref_id]) {
      buildLink(ouMappings, ouMappings[obj?.parent_ref_id].id, ouGroupId);
    }
  }
  return { ouGroupId };
};
