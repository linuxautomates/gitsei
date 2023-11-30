import { basicMappingType } from "dashboard/dashboard-types/common-types";
import {
  engineerFeatureResponsesType,
  engineerSectionType,
  OUUserDevProdConfig,
  scoreOverviewUtilType
} from "dashboard/dashboard-types/engineerScoreCard.types";
import { forEach, get } from "lodash";
import { engineerCategoryMapping } from "./constants";
import { findRatingByScore } from "../../helpers/devProductivityRating.helper";
import { engineerRatingType, ratingToColorMapping } from "../../constants/devProductivity.constant";

export const calculateIndividualSectionScore = (data: any) => {
  if (!data?.apidata) return {};
  const { categoryLayer, apidata } = data;
  const sections: engineerSectionType[] = get(apidata, ["section_responses"], []);
  const corrSec = sections.find((section: any) => {
    const secName = get(engineerCategoryMapping, [section.name], section.name);
    return secName === categoryLayer;
  });
  let finalScore = 0,
    featureMapping: engineerFeatureResponsesType[] = [],
    rating = "";
  if (corrSec) {
    featureMapping = get(corrSec, ["feature_responses"], []);
    const score = get(corrSec, ["score"], 0);
    finalScore = typeof score === "number" ? Math.round(score) : score;
    rating = findRatingByScore(finalScore);
  }
  return {
    finalScore: finalScore,
    rating: rating,
    color: ratingToColorMapping[rating as engineerRatingType],
    featureMapping: featureMapping
  };
};

export const calculateTotalEngineerScoreMapping = (data: any) => {
  if (!data) return {} as scoreOverviewUtilType;
  if (Array.isArray(data) && data.length) {
    data = data[0];
  }

  const sections: engineerSectionType[] = get(data, ["section_responses"], []);
  let sectionMapping: any = {};
  forEach(sections, section => {
    const secName = engineerCategoryMapping[section?.name] ?? section?.name;
    const sectionCalc = calculateIndividualSectionScore({ apidata: data, categoryLayer: secName });
    sectionMapping[secName] = sectionCalc;
  });
  const score = get(data, ["score"], "N/A");
  let finalScore = typeof score === "number" ? Math.round(score) : score;
  let rating = findRatingByScore(finalScore);
  return {
    finalScore: finalScore,
    rating: rating,
    color: ratingToColorMapping[rating as engineerRatingType],
    sectionMapping: sectionMapping
  } as scoreOverviewUtilType;
};

export const transformScoreTableData = (data: any) => {
  if (!data) return [];
  let finalData: OUUserDevProdConfig[] = [];
  forEach(data || [], record => {
    const sections: engineerSectionType[] = get(record, ["section_responses"], []);
    let categoryMapping: basicMappingType<number | "N/A"> = {};

    forEach(sections, section => {
      const score = get(section, ["score"], "N/A");
      let finalScore = typeof score === "number" ? Math.round(score) : score;
      categoryMapping[engineerCategoryMapping[section?.name] ?? section?.name] = finalScore as any;
    });

    finalData.push({
      ou_user_uuid: record?.org_user_id,
      name: record?.full_name ?? "",
      total_score: record?.score ?? 0,
      ...categoryMapping
    });
  });
  return finalData;
};
