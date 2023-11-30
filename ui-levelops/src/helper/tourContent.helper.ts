export enum TourType {
  PROPELS = "PROPELS",
  WIDGETS = "WIDGETS",
  OUDASHBOARD = "OUDASHBOARD",
  TOUR = "TOUR",
  SETTINGS = "SETTINGS"
}

export const getTourTitle = (type: TourType) => {
  switch (type) {
    case TourType.PROPELS:
      return "PROPELS";
    case TourType.OUDASHBOARD:
      return "COLLECTIONS AND DASHBOARDS";
    case TourType.SETTINGS:
      return "SETTINGS";
    case TourType.WIDGETS:
      return "DevOps (DORA) METRICS";
    case TourType.TOUR:
      return "TOUR NEW FEATURES";
    default:
      return TourType.TOUR;
  }
};

export const getTourDescription = (type: TourType) => {
  let description = {
    desc1: "",
    desc2: "",
    desc3: ""
  };

  switch (type) {
    case TourType.PROPELS: {
      description.desc1 =
        "Propels are human-in-the-loop automation that helps you automate repetitive tasks and brings in human judgement at critical decision points.";
      description.desc2 =
        "For example, before you push a release into production, get approval from all the stakeholders such as Dev, QA, Tech pubs, SRE etc. and then automatically promote it to production.";
      return description;
    }
    case TourType.OUDASHBOARD: {
      description.desc1 = "The Insight contains a cluster of related widgets. Switch between all the Insights here.";
      description.desc2 =
        "Collections are the team structures based on location, repos, pods, hierarchy etc., defined in the system. Switch between all the Collections here.";
      description.desc3 = "You may also create and edit the Insights and the Collections under this menu.";
      return description;
    }
    case TourType.SETTINGS: {
      description.desc1 = "Find all your configurations under the settings menu.";
      description.desc2 =
        "You will be able to create collections, workflow profiles, add new integrations and much more.";
      return description;
    }
    case TourType.WIDGETS: {
      description.desc1 = "There are 4 key metrics that many collections use to measure their delivery performance.";
      description.desc2 =
        "These metrics were suggested by the DevOps Research & Assessment (DORA) research program and hence are known as DORA metrics.";
      return description;
    }
    case TourType.TOUR: {
      description.desc1 = "Click here to turn this tour on and off.";
      return description;
    }
    default: {
      return description;
    }
  }
};
