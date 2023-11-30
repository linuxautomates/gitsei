import React, { useEffect } from "react";
import ReactGA from "react-ga";
import { connect } from "react-redux";
import { mapSessionStatetoProps } from "reduxConfigs/maps/sessionMap";
import * as GA from "../dataTracking/google-analytics";
import { sessionCurrentUserState } from "reduxConfigs/selectors/session_current_user.selector";
import { useSelector } from "react-redux";

import envConfig from "env-config";
import LocalStoreService from "../services/localStoreService";
const ls = new LocalStoreService();
const companyName = ls.getUserCompany();
const userName = ls.getUserName();

type LocationObject = {
  [key: string]: any;
};

export interface HOCProps {
  location: LocationObject;
  session_license: string;
}

const withTracker = (WrappedComponent: React.ComponentType<any>) => {
  // eslint-disable-next-line

  const HOC = (props: HOCProps) => {
    const sessionCurrentUser = useSelector(sessionCurrentUserState);

    useEffect(() => {
      if (!!sessionCurrentUser.id) {
        // initialize GA
        GA.start(sessionCurrentUser);

        // eslint-disable-next-line
        const page = props.location.pathname + props.location.search;

        ReactGA.set({ dimension1: companyName });
        ReactGA.set({ dimension2: envConfig.get("NODE_ENV") });
        ReactGA.set({ dimension3: userName });
        ReactGA.pageview(page);

        const userLicense = props.session_license;
        if (userLicense) {
          ReactGA.set({ dimension4: userLicense });
        }
      }
    }, [sessionCurrentUser]);

    useEffect(() => {
      ReactGA.pageview(props.location.pathname + props.location.search);
    }, [props.location]);

    return <WrappedComponent {...props} />;
  };

  return connect(mapSessionStatetoProps)(HOC as any);
};

export default withTracker;
