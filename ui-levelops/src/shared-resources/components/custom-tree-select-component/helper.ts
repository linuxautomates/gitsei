export const structureProvidedData = (data: any[] = []) => {
  return data.map(_datum => {
    let { key } = _datum;
    if (!key) {
      key = _datum?.value;
    }
    const to = key.lastIndexOf("\\");
    const parentKey = to !== -1 ? key.substring(0, to) : undefined;
    const childKey = to == -1 ? key : key.substring(to + 1, key.length);
    return {
      key: childKey,
      value: key,
      parent_key: parentKey
    };
  });
};
