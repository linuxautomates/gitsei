package io.levelops.api.controllers;

//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(classes = {TeamsController.class, DefaultApiTestConfiguration.class})
public class TeamsControllerTest {
//    private MockMvc mvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//    @Autowired
//    private TeamService teamService;
//
//    private TeamsController controller;
//
//
//    @Before
//    public void setup() {
//        //The non-standalone setup will require authentication and everything to be done properly.
//        controller = new TeamsController(objectMapper, teamService);
//        mvc = MockMvcBuilders.standaloneSetup(controller).build();
//    }
//
//    @Test
//    public void testCreateTeam() throws Exception {
//        mvc.perform(post("/v1/teams").contentType(MediaType.APPLICATION_JSON)
//                .sessionAttr("company", "test")
//                .content("{\"organization_id\":\"2\",\"name\":\"test1\"}"))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    public void testGetTeam() throws Exception {
//        Team team = Team.builder().name("team1").id("1").organizationId("1").build();
//        MvcResult result = mvc.perform(get("/v1/teams/1").contentType(MediaType.APPLICATION_JSON)
//                .sessionAttr("company", "test")).andDo(MockMvcResultHandlers.print()).andReturn();
//        mvc.perform(asyncDispatch(result))
//                .andExpect(status().isOk())
//                .andExpect(content().json(objectMapper.writeValueAsString(team)));
//    }
}
