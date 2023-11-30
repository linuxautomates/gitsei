import LazyWithSuspense from "hoc/LazyWithSuspense/LazyWithSuspense";
import React from "react";

const TicketCategorizationSchemesLandingPageLazy = LazyWithSuspense(
  React.lazy(() => import("./TicketCategorizationProfile"))
);

export default TicketCategorizationSchemesLandingPageLazy;
