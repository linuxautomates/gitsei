import { createStore, applyMiddleware } from "redux";
import { composeWithDevTools } from "redux-devtools-extension";
import { rootReducer } from "reduxConfigs";
import rootSaga from "reduxConfigs/sagas";
import createSagaMiddleware from "redux-saga";
import { persistStore, persistReducer } from "redux-persist";
import storage from "redux-persist/lib/storage";

const persistConfig = {
  key: "root:cachedState",
  whitelist: ["cachedStateReducer", "cachedIntegrationReducer"],
  storage
};

const persistedReducer = persistReducer(persistConfig, rootReducer);

/* Root Saga Middleware*/
const sagaMiddleware = createSagaMiddleware();

let middlewares = applyMiddleware(sagaMiddleware);

/* Root Store with all the combined reducers*/

const store = createStore(persistedReducer, composeWithDevTools(middlewares));

export const persistor = persistStore(store);
/*Run the sagas*/
sagaMiddleware.run(rootSaga);

export default store;
