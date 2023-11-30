import { RestTrellisScoreProfile } from "classes/RestTrellisProfile";

export const sortProfiles = (trellisScoreProfilesList: RestTrellisScoreProfile[] | undefined) => {
  if (!(trellisScoreProfilesList || []).length) return [];

  const filteredProfiles = trellisScoreProfilesList?.reduce(
    (acc: any, profile: RestTrellisScoreProfile) => {
      if (profile.predefined_profile) {
        return {
          ...acc,
          predefined: [...acc.predefined, profile]
        };
      }
      return {
        ...acc,
        others: [...acc.others, profile]
      };
    },
    {
      predefined: [],
      others: []
    }
  );

  return filteredProfiles.predefined.concat(filteredProfiles.others);
};
