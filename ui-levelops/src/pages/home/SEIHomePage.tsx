/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

import React, { useEffect } from "react";

import { useHistory, useParams } from "react-router-dom";
import { pick } from "lodash";
import { PageError, PageSpinner } from "@harness/uicore";
import bgImageURL from "./images/sei.png";
import { useLicenseStoreContext } from "contexts/LicenseStoreContext";
import { AccountPathProps } from "@harness/microfrontends/dist/modules/10-common/interfaces/RouteInterfaces";
import { useParentProvider } from "contexts/ParentProvider";
import { Project } from "@harness/microfrontends/dist/services/cd-ng";

const SEIHomePage: React.FC = () => {
  const {
    components: { HomePageTemplate },
    routes,
    services: { useGetLicensesAndSummary }
  } = useParentProvider();
  const history = useHistory();
  const { licenseInformation, updateLicenseStore } = useLicenseStoreContext();

  const { accountId } = useParams<AccountPathProps>();
  const moduleType = "sei";
  const module = "sei";

  const { data, error, refetch, loading } = useGetLicensesAndSummary({
    queryParams: { moduleType: moduleType },
    accountIdentifier: accountId,
    lazy: true
  });

  useEffect(() => {
    const newLicenseInformation = {
      ...licenseInformation,
      sei: data
    };
    const licenseStoreData = {
      licenseInformation: newLicenseInformation
    };
    updateLicenseStore(licenseStoreData);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data]);

  if (loading) {
    return <PageSpinner />;
  }

  if (error) {
    const message = (error?.data as Error)?.message || error?.message;
    return <PageError message={message} onClick={() => refetch()} />;
  }

  if (data?.status === "SUCCESS" && !data.data) {
    history.push(
      routes.toModuleTrialHome({
        accountId,
        module
      })
    );
  }

  // projectCreateSuccessHandler: redirects to the project dashboard upon selection from new project modal
  const projectCreateSuccessHandler = (project?: Project): void => {
    if (project) {
      history.push(
        (routes as any).toProjectOverview({
          projectIdentifier: project.identifier,
          orgIdentifier: project.orgIdentifier || "",
          accountId,
          module
        })
      );
    }
  };

  return (
    <HomePageTemplate
      title={"Software Engineering Insights"}
      bgImageUrl={bgImageURL}
      projectCreateSuccessHandler={projectCreateSuccessHandler}
      subTitle={
        "Improve software quality, developer experience, and time to value by gaining actionable insights into software delivery, workflows, and systems."
      }
      documentText={" Learn more"}
      documentURL={"https://www.harness.io/products/software-engineering-insights"}
    />
  );
};

export default SEIHomePage;
