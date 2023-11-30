import LazyWithSuspense from "hoc/LazyWithSuspense/LazyWithSuspense";
import React from "react";

const ProfileContainerLazy = LazyWithSuspense(React.lazy(() => import("./ProfileContainer")));

export default ProfileContainerLazy;
