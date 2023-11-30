import memoizeOne from "memoize-one";
import { engineerRatingType, ratingToLegendColorMapping } from "../constants/devProductivity.constant";

export const findRatingByScore = (score: number | string | null) => {
  if ((!score && score !== 0) || score === "N/A") {
    return engineerRatingType.NO_SCORE;
  }
  if (score >= 75) return engineerRatingType.GOOD;
  if (score > 50 && score < 75) return engineerRatingType.ACCEPTABLE;
  return engineerRatingType.NEED_IMPROVEMENT;
};

export const findRatingByScoreType = (score: string | null) => {
  if (!score || score === "N/A") {
    return engineerRatingType.NO_SCORE;
  }
  const _rating = score?.toLowerCase();
  switch (_rating) {
    case "acceptable":
      return engineerRatingType.ACCEPTABLE;
    case "good":
      return engineerRatingType.GOOD;
    default:
      return engineerRatingType.NEED_IMPROVEMENT;
  }
};

export const legendColorByScore = memoizeOne((item: number) => {
  return ratingToLegendColorMapping[findRatingByScore(item)];
});

export const legendColorByRating = memoizeOne((item: engineerRatingType) => {
  return ratingToLegendColorMapping[item];
});
