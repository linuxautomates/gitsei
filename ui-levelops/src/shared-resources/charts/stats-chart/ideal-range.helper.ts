export const STAT_BG_WHITE = "#fff";
export const STAT_BG_BETWEEN_IDEAL_RANGE = "#f4ffec";
export const STAT_BG_OUTSIDE_IDEAL_RANGE = "#ffeff4";

export const getIdealRangeBGColor = (idealRange: { min: number; max: number }, stat: number): string => {
  // This is case for
  // 1. Ideal Range is not defined
  // 2. Either MIN or MAX is not defined
  if (idealRange.min === Number.MIN_SAFE_INTEGER && idealRange.max === Number.MAX_SAFE_INTEGER) {
    return STAT_BG_WHITE;
  }
  if (stat < idealRange.min || stat > idealRange.max) {
    return STAT_BG_OUTSIDE_IDEAL_RANGE;
  }
  return STAT_BG_BETWEEN_IDEAL_RANGE;
};
