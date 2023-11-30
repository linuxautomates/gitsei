export const getModulePathString = (breadCrumbs: string[], index: number) => {
  let modulePathArray = [];
  let currentIndex = 0;
  while (currentIndex <= index) {
    modulePathArray.push(breadCrumbs[currentIndex++]);
  }
  // to remove 'root' from path, we use slice(1)
  return modulePathArray.slice(1).join("/");
};

export const isFile = (name: string) => {
  if (!name || name === "...") {
    return false;
  }
  return (name || "").split("").includes(".");
};

// Returns the lighter shade of color based on percent
// colorTint("#ff6b6a", 15) => #ff8180
export const colorTint = (color: string, percent: number) => {
  color = color.replace(/^#/, "");
  if (color.length === 3) color = color[0] + color[0] + color[1] + color[1] + color[2] + color[2];

  let [r, g, b]: any[] = color.match(/.{2}/g) || ["0", "0", "0"];
  [r, g, b] = [parseInt(r, 16), parseInt(g, 16), parseInt(b, 16)];

  [r, g, b] = [r + ((256 - r) * percent) / 100, g + ((256 - g) * percent) / 100, b + ((256 - b) * percent) / 100];

  r = Math.max(Math.min(255, r), 0).toString(16);
  g = Math.max(Math.min(255, g), 0).toString(16);
  b = Math.max(Math.min(255, b), 0).toString(16);

  const rr = (r.length < 2 ? "0" : "") + r.substring(0, 2);
  const gg = (g.length < 2 ? "0" : "") + g.substring(0, 2);
  const bb = (b.length < 2 ? "0" : "") + b.substring(0, 2);
  return `#${rr}${gg}${bb}`;
};
