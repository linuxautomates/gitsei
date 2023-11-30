import React, { Suspense } from "react";

const LazyWithSuspense = (WrappedComponent: React.ComponentType) => {
  return class extends React.Component {
    render() {
      return (
        <Suspense fallback={<div>Loading...</div>}>
          <WrappedComponent {...this.props} />
        </Suspense>
      );
    }
  };
};

export default LazyWithSuspense;
