import { RouteComponentProps } from "react-router-dom";
import ErrorWrapper from "../../../hoc/errorWrapper";
import React, { useEffect, useRef, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import {
  integrationsGet,
  integrationsList,
  mappingsCreate,
  mappingsDelete,
  mappingsList,
  productsCreate,
  productsGet,
  productsUpdate,
  usersGet
} from "reduxConfigs/actions/restapi";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import {
  MappingsSelectorState,
  ProductsSelectorState,
  UserSelectorState,
  IntegrationsSelectorState
} from "reduxConfigs/selectors/restapiSelector";
import { RestMapping, RestProduct } from "../../../classes/RestProduct";
import { AntForm, AntFormItem, AntInput, AntModal, AntText } from "shared-resources/components";
import { SelectRestapi } from "shared-resources/helpers";
import { EMPTY_FIELD_WARNING, ERROR } from "constants/formWarnings";
import { isEmpty } from "lodash";
import { WORKSPACES, WORKSPACE_NAME_MAPPING } from "dashboard/constants/applications/names";

export interface ProductMappingsContainerProps extends RouteComponentProps {
  product_id: any;
  onUpdate: Function;
  display: boolean;
  onCancel: Function;
}

const ProductMappingsContainer: React.FC<ProductMappingsContainerProps> = (props: ProductMappingsContainerProps) => {
  const dispatch = useDispatch();
  const productsApiState = useSelector(state => ProductsSelectorState(state));
  const mappingsApiState = useSelector(state => MappingsSelectorState(state));
  const usersApiState = useSelector(state => UserSelectorState(state));
  const integrationsApiState = useSelector(state => IntegrationsSelectorState(state));

  const productIdRef = useRef<any>(undefined);
  const nameFieldRef = useRef<any>(undefined);
  const ownerFieldRef = useRef<any>(undefined);
  const keyFieldRef = useRef<any>(undefined);

  const [productLoading, setProductLoading] = useState<boolean>(false);
  const [userLoading, setUserLoading] = useState<boolean>(false);
  const [mappingsLoading, setMappingsLoading] = useState<boolean>(false);
  const [integrationsLoading, setIntegrationsLoading] = useState<boolean>(false);
  const [createProduct, setCreateProduct] = useState<boolean>(false);

  const [product, setProduct] = useState<any>(new RestProduct());
  const [userSelect, setUserSelect] = useState<any>(undefined);
  const [integrationsSelect, setIntegrationsSelect] = useState<any>([]);

  useEffect(() => {
    if (props.product_id !== undefined) {
      productIdRef.current = props.product_id;
      setProductLoading(true);
      setMappingsLoading(true);
      dispatch(productsGet(productIdRef.current));
      dispatch(mappingsList({ filter: { product_id: productIdRef.current } }));
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    return () => {
      dispatch(restapiClear("mappings", "list", "0"));
      dispatch(restapiClear("products", "create", "-1"));
      dispatch(restapiClear("products", "update", "-1"));
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (productLoading) {
      // @ts-ignore
      const { loading, error } = productsApiState?.get[productIdRef.current] || {};
      if (loading !== undefined && error !== undefined && !loading && !error) {
        const { data: product } = productsApiState?.get[productIdRef.current];
        if (product.owner_id !== undefined) {
          dispatch(usersGet(product.owner_id));
        }
        setProductLoading(false);
        setUserLoading(product.owner_id !== undefined);
        // @ts-ignore
        setProduct(new RestProduct(productsApiState?.get[productIdRef.current]?.data));
      }
    }
  }, [productLoading, productsApiState?.get[productIdRef.current]]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (userLoading) {
      const { loading, error, data: user } = usersApiState?.get[product.owner_id] || {};
      if (loading !== undefined && !loading) {
        if (!error) {
          setUserLoading(false);
          setUserSelect({ key: user.id, label: user.email });
        } else {
          setUserLoading(false);
        }
      }
    }
  }, [userLoading, usersApiState?.get[product.owner_id]]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (mappingsLoading) {
      const { loading, error } = mappingsApiState?.list || {};
      if (loading !== undefined && error !== undefined && !loading && !error) {
        const mappings = mappingsApiState?.list?.data.records;
        if (mappings.length > 0) {
          mappings.forEach((mapping: any) => {
            dispatch(integrationsGet(mapping.integration_id));
          });
        }
        setMappingsLoading(false);
        setIntegrationsLoading(mappings.length > 0);
      }
    }
  }, [mappingsLoading, mappingsApiState?.list]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (integrationsLoading) {
      const mappings = mappingsApiState?.list?.data?.records;
      let integrations_loading = false;
      let integrationsSelect: any[] = [];

      if (mappings) {
        mappings.forEach((mapping: any) => {
          const integrationState = integrationsApiState?.get?.[mapping.integration_id] || {};
          if (!integrationState || isEmpty(integrationState)) {
            integrations_loading = true;
            // Don't continue executing this forEach
            // callback function for this mapping.
            return;
          }

          const { loading, error } = integrationState;
          if (loading || error) {
            integrations_loading = true;
          } else {
            const integration = integrationState.data;
            integrationsSelect.push({ label: integration?.name, key: integration?.id });
          }
        });
      } else {
        integrations_loading = true;
      }

      if (!integrations_loading) {
        setIntegrationsLoading(false);
        setIntegrationsSelect(integrationsSelect);
      }
    }
  }, [integrationsLoading, mappingsApiState?.list, integrationsApiState?.get]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (createProduct) {
      const method = productIdRef.current === undefined ? "create" : "update";
      let id = productIdRef.current || "0";
      const { loading, error } = productsApiState?.[method]?.[id] || {};
      if (loading !== undefined && error !== undefined && !loading && !error) {
        if (productIdRef.current === undefined) {
          id = productsApiState?.[method]?.[id].data?.id;
          integrationsSelect.forEach((integration: any) => {
            let map = new RestMapping();
            map.product_id = id;
            map.integration_id = integration.key;
            dispatch(mappingsCreate(map));
          });
        }
        dispatch(productsGet(id));
        props.onUpdate();
        setProductLoading(productIdRef.current !== undefined);
        // ...ProductMappingsContainer.defaultState(),
      }
    }
  }, [createProduct, productsApiState]); // eslint-disable-line react-hooks/exhaustive-deps

  function onChangeHandler(field: string) {
    return (e: any) => {
      switch (field) {
        case "name":
          product.name = e.currentTarget.value;
          break;
        case "description":
          product.description = e.currentTarget.value;
          break;
        case "owner":
          product.owner_id = e ? e.key : undefined;
          setUserSelect(e || {});
          break;
        case "key":
          product.key = e.currentTarget.value;
          break;
        default:
          break;
      }
      setProduct(new RestProduct(product));
    };
  }

  function onUpdate() {
    // first figure out how many mappings need to be deleted vs added
    setCreateProduct(true);
    if (productIdRef.current !== undefined) {
      const mappingsList = mappingsApiState?.list?.data?.records;
      const mappings = mappingsList.map((map: any) => map.integration_id);
      const newIntegrations = integrationsSelect
        .filter((integration: any) => !mappings.includes(integration.key))
        .map((i: any) => i.key);
      const deletedIntegrations = mappings.filter(
        (map: any) => !integrationsSelect.map((i: any) => i.key).includes(map)
      );
      dispatch(productsUpdate(productIdRef.current, product));
      newIntegrations.forEach((integration: any) => {
        let map = new RestMapping();
        map.product_id = productIdRef.current;
        map.integration_id = integration;
        dispatch(mappingsCreate(map));
      });
      deletedIntegrations.forEach((integration: any) => {
        const mappingsList = mappingsApiState?.list?.data?.records;
        const map = mappingsList.filter((map: any) => map.integration_id === integration);
        dispatch(mappingsDelete(map[0].id));
      });
    } else {
      dispatch(productsCreate(product));
    }
  }
  function handleMappingsChange(value: any) {
    setIntegrationsSelect(value || []);
  }

  function okButtonProps() {
    const valid =
      product.name !== undefined &&
      product.name.trim() !== "" &&
      product.key !== undefined &&
      product.key.trim() !== "" &&
      product.owner_id !== undefined;
    return { disabled: !valid };
  }

  return (
    <AntModal
      title={WORKSPACE_NAME_MAPPING[WORKSPACES]}
      visible={props.display}
      onCancel={props.onCancel}
      okButtonProps={okButtonProps()}
      onOk={onUpdate}
      okText={"Save"}>
      <AntForm layout={"vertical"}>
        {productIdRef.current !== undefined && (
          <AntFormItem label={"workspace id"}>
            <AntText copyable>{productIdRef.current}</AntText>
          </AntFormItem>
        )}

        <AntFormItem
          label={"Name"}
          colon={false}
          required={true}
          validateStatus={nameFieldRef.current && product.name === "" ? ERROR : ""}
          hasFeedback={true}
          help={nameFieldRef.current && product.name === "" && EMPTY_FIELD_WARNING}>
          <AntInput
            name={"name"}
            value={product.name}
            onChange={onChangeHandler("name")}
            onBlur={(e: any) => {
              nameFieldRef.current = true;
            }}
          />
        </AntFormItem>
        <AntFormItem label={"Description"} colon={false} required={false} hasFeedback={false}>
          <AntInput name={"description"} value={product.description} onChange={onChangeHandler("description")} />
        </AntFormItem>
        <AntFormItem label={"Owner"} colon={false} required={true} hasFeedback={true}>
          <SelectRestapi
            placeholder={"Project Owner"}
            value={userSelect}
            mode={"single"}
            labelInValue={true}
            uri={"users"}
            searchField={"email"}
            onChange={onChangeHandler("owner")}
            onBlur={(e: any) => {
              ownerFieldRef.current = true;
            }}
          />
        </AntFormItem>
        <AntFormItem
          label={"Key"}
          colon={false}
          required={true}
          hasFeedback={true}
          //help={this.state.owner && !validateEmail(product.owner) && EMAIL_WARNING}
          validateStatus={keyFieldRef.current && product.key === "" ? ERROR : ""}>
          <AntInput
            name={"key"}
            disabled={product.id !== undefined}
            value={product.key}
            onChange={onChangeHandler("key")}
            placeholder={"KEY"}
            onBlur={(e: any) => {
              keyFieldRef.current = true;
            }}
          />
        </AntFormItem>
        <AntFormItem label={"Integration Mappings"} colon={false}>
          <SelectRestapi
            searchField="name"
            uri="integrations"
            fetchData={(filters: any, complete: any) => dispatch(integrationsList(filters, complete))}
            method="list"
            //rest_api={this.props.rest_api}
            isMulti={false}
            closeMenuOnSelect
            value={integrationsSelect}
            creatable={false}
            mode={"multiple"}
            labelInValue
            onChange={handleMappingsChange}
          />
        </AntFormItem>
      </AntForm>
    </AntModal>
  );
};

export default ErrorWrapper(ProductMappingsContainer);
