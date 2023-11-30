import React, { useEffect } from "react";
import { Container, Text } from "@harness/uicore";
import { useGetIngestionStatusMutation } from "@harnessio/react-sei-service-client";
import { tableCell } from "utils/tableUtils";
import StatusBadge from "./StatusBadge";

const Status = ({ id }: any) => {
  const { mutate, isLoading, data, isError } = useGetIngestionStatusMutation();
  useEffect(() => {
    mutate({
      integrationId: id,
      body: {}
    });
  }, []);

  return (
    <Container padding={{ left: "large" }} key={id}>
      {isLoading && <Text icon="loading">Loading</Text>}
      {!isLoading && isError && <Text>{"Could not fetch"}</Text>}
      {!isLoading && !isError && data && <StatusBadge status={(data.content as any).status} />}
    </Container>
  );
};

export default Status;
