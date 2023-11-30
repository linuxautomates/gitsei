import LazyWithSuspense from "hoc/LazyWithSuspense/LazyWithSuspense";
import React from "react";

const CategoriesContainerLazy = LazyWithSuspense(React.lazy(() => import("./CategoriesContainer")));

export default CategoriesContainerLazy;
