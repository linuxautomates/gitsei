package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.State;
import io.levelops.commons.databases.models.database.WorkItem;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

public class StateDBServiceTest {
    private static final String STATE_NAME_PREFIX = "state-name-";

    private final String company = "test";

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private DataSource dataSource;
    private StateDBService stateDBService;


    @Before
    public void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        stateDBService = new StateDBService(dataSource);

        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        stateDBService.ensureTableExistence(company);
    }

    private void verifyState(State e, State a){
        Assert.assertEquals(e.getId(), a.getId());
        Assert.assertEquals(e.getName(), a.getName());
    }
    private void verifyStates(List<State> e, List<State> a){
        Assert.assertEquals(CollectionUtils.isEmpty(e),CollectionUtils.isEmpty(a));
        if(CollectionUtils.isEmpty(e)){
            return;
        }
        Assert.assertEquals(e.size(),a.size());
        Map<Integer, State> em = e.stream().collect(Collectors.toMap(State::getId, v->v));
        Map<Integer, State> am = a.stream().collect(Collectors.toMap(State::getId, v->v));
        for(State t : e){
            verifyState(em.get(t.getId()), am.get(t.getId()));
        }
    }

    private State testCreateState(int n) throws SQLException {
        State state = State.builder()
                .name(STATE_NAME_PREFIX+n)
                .build();
        String stateId = stateDBService.insert(company,state);
        Assert.assertNotNull(stateId);
        return state.toBuilder().id(Integer.parseInt(stateId)).build();
    }
    private void testGetState(List<State> states) throws SQLException {
        for(State st : states){
            Optional<State> optionalState = stateDBService.get(company, st.getId().toString());
            Assert.assertNotNull(optionalState);
            Assert.assertTrue(optionalState.isPresent());
            State actual = optionalState.get();
            verifyState(st, actual);
        }
    }

    private void testListState(List<State> states) throws SQLException {
        DbListResponse<State> dbListResponse = stateDBService.list(company, 0, 50);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(states.size(), dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(states.size(), dbListResponse.getCount().intValue());
        verifyStates(states, dbListResponse.getRecords());
    }

    private State modifyState(State s){
        return s.toBuilder()
                .name(s.getName() + "-modified")
                .build();
    }

    private State testModifyState(State s) throws SQLException {
        State modified = modifyState(s);
        Boolean res = stateDBService.update(company, modified);
        Assert.assertTrue(res);
        return modified;
    }

    @Test
    public void test() throws SQLException {
        List<State> states = new ArrayList<>();
        states.addAll(stateDBService.list(company, 0, 10).getRecords());
        //Init Rows = 3
        Assert.assertEquals(WorkItem.ItemStatus.values().length, states.size());
        for(int i=0; i <2;i++) {
            states.add(testCreateState(i));
            testGetState(states);
            testListState(states);
        }
        Boolean deleteResult = stateDBService.delete(company, states.get(0).getId().toString());
        Assert.assertTrue(deleteResult);
        states.remove(0);

        testGetState(states);
        testListState(states);

        State modified = testModifyState(states.get(0));
        states.set(0,modified);

        testGetState(states);
        testListState(states);
    }

    private Set<String> getExpectedNames(List<State> states){
        Set<String> stateNames = new HashSet<>();
        if(CollectionUtils.isNotEmpty(states)) {
            for (State st : states) {
                stateNames.add(st.getName());
            }
        }
        for(WorkItem.ItemStatus s : WorkItem.ItemStatus.values()){
            stateNames.add(s.toString());
        }
        return stateNames;
    }
    @Test
    public void testListByFilter() throws SQLException {
        List<State> states = new ArrayList<>();
        for(WorkItem.ItemStatus s : WorkItem.ItemStatus.values()){
            State st = State.builder().name("test-" + s.toString()).build();
            String id = stateDBService.insert(company, st);
            states.add(st.toBuilder().id(Integer.parseInt(id)).build());
        }
        for(State s: states){
            DbListResponse<State> dbListResponse = stateDBService.listByFilter(company, null, s.getName(), 0, 10);
            Assert.assertNotNull(dbListResponse);
            Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
            Assert.assertEquals(1, dbListResponse.getCount().intValue());
            verifyState(s, dbListResponse.getRecords().get(0));

            State actual = stateDBService.getStateByName(company, s.getName());
            verifyState(actual, s);
        }
        DbListResponse<State> dbListResponse = stateDBService.listByFilter(company, null,null, 0, 10);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(WorkItem.ItemStatus.values().length *2, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(WorkItem.ItemStatus.values().length *2, dbListResponse.getCount().intValue());
        Assert.assertEquals(WorkItem.ItemStatus.values().length *2, dbListResponse.getRecords().size());
        Assert.assertEquals(getExpectedNames(states), dbListResponse.getRecords().stream().map(s -> s.getName()).collect(Collectors.toSet()));

        dbListResponse = stateDBService.listByFilter(company, null,null, 0, 2);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(WorkItem.ItemStatus.values().length*2, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(2, dbListResponse.getCount().intValue());
        Assert.assertEquals(2, dbListResponse.getRecords().size());
    }

    @Test
    public void testBootStrap() throws SQLException {
        for(int i=0; i <3; i++) {
            stateDBService.ensureTableExistence(company);
        }
        List<State> states = new ArrayList<>();
        states.addAll(stateDBService.list(company, 0, 10).getRecords());
        //Init Rows = 3
        Assert.assertEquals(WorkItem.ItemStatus.values().length, states.size());
        Assert.assertEquals(getExpectedNames(null), states.stream().map(s -> s.getName()).collect(Collectors.toSet()));
    }
}