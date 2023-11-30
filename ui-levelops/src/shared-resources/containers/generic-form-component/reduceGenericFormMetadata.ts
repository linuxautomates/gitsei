import { get } from "lodash";

export function reduceGenericFormMetadata(metadata: any = {}) {
  // @ts-ignore
  const elements = metadata.elements || [];
  const result = elements.reduce((acc: any, obj: any) => {
    switch (obj.type) {
      case "checkbox-group":
        const keys = get(metadata, [obj.key, "value", 0], []);
        keys.forEach((val: string) => {
          acc[val] = true;
        });
        break;
      case "text":
        acc[obj.key] = get(metadata, [obj.key, "value", 0], "");
        break;
      default:
        acc[obj.key] = metadata[obj.key].value;
    }
    return acc;
  }, {});

  return result;
}
