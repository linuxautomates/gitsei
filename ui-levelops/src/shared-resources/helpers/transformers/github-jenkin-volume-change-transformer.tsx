export const volumeChangeTransformer = (value: string | number, key: string) => {
  let val = value;

  if (!value) {
    return 0;
  }

  if (typeof val === "string") {
    val = parseInt(val);
  }

  if (key === "lines_removed_count" || key.includes("lines_removed_count")) {
    return val < 0 ? val * -1 : val;
  }

  return val;
};
