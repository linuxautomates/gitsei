import React from "react";
import * as PropTypes from "prop-types";
import { useDispatch, useSelector } from "react-redux";
import { Card } from "shared-resources/components";
import { Title } from "shared-resources/helpers";
import { SelectWrapper } from "shared-resources/helpers";
import SearchableSelect from "components/SearchableSelect/SearchableSelect";
import { getPolicyForm } from "reduxConfigs/selectors/formSelector";
import { formUpdateObj } from "reduxConfigs/actions/formActions";
import { get } from "lodash";
import { cTemplatesList } from "reduxConfigs/actions/restapi";
interface SignaturesContainerProps {}
export const SignaturesContainer: React.FC<SignaturesContainerProps> = (props: SignaturesContainerProps) => {
  const dispatch = useDispatch();

  const policy_form = useSelector(state => getPolicyForm(state));

  const rest_api = useSelector(state => get(state, ["restapiReducer"], {}));

  return (
    <Card>
      <Title title={"Signatures"} button={false} />
      <div className={`flex direction-row justify-start`} style={{ marginTop: "20px" }}>
        <div style={{ flexBasis: "30%" }}>
          <SelectWrapper label={"signatures"}>
            <SearchableSelect
              searchField="name"
              uri="ctemplates"
              fetchData={dispatch(cTemplatesList)}
              method="list"
              rest_api={rest_api}
              isMulti={true}
              closeMenuOnSelect={true}
              value={policy_form.signatures || []}
              creatable={false}
              onChange={(options: any) => {
                let policy = policy_form;
                policy.signatures = options;
                dispatch(formUpdateObj("policy_form", policy));
              }}
              placeholder={"Select Signatures"}
            />
          </SelectWrapper>
        </div>
      </div>
    </Card>
  );
};

export default React.memo(SignaturesContainer);
