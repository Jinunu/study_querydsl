package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory ;

    @BeforeEach
    public void beFore( ){
        queryFactory =  new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);


        //when
        em.flush();
        em.clear();

        List<Member> members = em.createQuery("select m from  Member m", Member.class)
                .getResultList();
        //then
        for (Member member : members) {
            System.out.println("member = " + member);
            System.out.println("member.team = " + member.getTeam());

        }
    }


    @Test
    public void startJPQL() throws Exception {
        //member1을 찾아라
        String qlString = "select m from Member  m " +
                "where m.username =: username";
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() throws Exception {
        //given
//        QMember m = new QMember("m");
//        QMember m = QMember.member;
//        Qmembr를 스태틱 임포트해서쓰기 !
//        why Querydsl as 가 member1이 들어가나 ? 자동으로 만들어줬기 때문에 new 생성하면 as 바꿀 수 있음 같은 테이블 조인 할 경우! 선언해서 쓰면됨
        //when
        Member findMember = queryFactory
                .selectFrom(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        //then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void serch() throws Exception {
        //given
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1").
                        and(member.age.eq(10)))
                .fetchOne();
        //then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void serchAndParam() throws Exception {
        //given
        //and는 체인으로 써도 되고 ,로 나눠도 된다. ,를쓰면 null을 무시한다 동적쿼리에 기가 막힌단다.
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();
        //then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFeth() throws Exception {
        //given
//        List<Member> fetch = queryFactory
//                .selectFrom(member)
//                .fetch();
//        Member fetchOne = queryFactory
//                .selectFrom(QMember.member)
//                .fetchOne();
//
//        Member fetchFirst = queryFactory
//                .selectFrom(QMember.member)
//                .fetchFirst();

//        QueryResults<Member> results = queryFactory
//                .selectFrom(member)
//                .fetchResults();
//        results.getTotal();
//        List<Member> content = results.getResults();

        long total = queryFactory
                .selectFrom(member)
                .fetchCount();


    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(desc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(null last)*/
    @Test
    public void sort() throws Exception {
      em.persist(new Member(null, 100));
      em.persist(new Member("member5", 100));
      em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging() throws Exception {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging2() throws Exception {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    @Test
    void aggregation() {
        //참고 여기서 Tuple 은 Querydsl이 제공하는 거다 그리고 튜플보다 DTO로 조회 하는경우가 더 많다고 한다
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);

    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     * */
    @Test
    public void group() throws Exception {
        //given
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        //when
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);
        //then
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);


    }
    /**
     * 팀 A에 소속된 모든 회원
     */
    @Test
    public void join() throws Exception {
        //given
        List<Member> result = queryFactory.selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();
        //when

        //then
        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }
    /**
     * 세타조인(막조인)  연관 관계가 없어도 조인이 가능하다. DB마다 성능 최적화를 한다.
     * 회원의 이름이 팀 이름과 같은 회원 조회
     * 외부 조인 불가능 -> 조인 on을 사용하면 외부 조인가능.
     * */
    @Test
    public void theta_join() throws Exception {
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        //when
        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();
        //then
        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 예) 회원과 팀을 조인하면서, 팀이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: select m, t from Member m letf join m.team t on t.name = 'teamA'
     * inner_join 이면 on절로 걸러 내나 where절로 걸러내나 똑같다 .
     * outer join 이면 on절로 풀자 단순한 inner join 이면 where로 풀자.
     * */
    @Test
    public void join_on_filtering() throws Exception {
        //given
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();
        //when
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
            
        }

        //then
    }

    /**
     * 연관관계 없는 엔티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     * */
    @Test
    @Commit
    public void join_on_no_relation() throws Exception {
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        //when
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name)) // .leftJoin(member.team, team).on(member.username.eq(team.name)) 원래는 연관관계가 있는 테이블을 넣어 id를 매칭시키지만 막조인은 그냥 카르테시안곱인가 그걸로 조인함.
                .fetch();
        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() throws Exception {
        //given
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @Test
    public void fetchJoinUse() throws Exception {
        //given
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    /**
    * 나이가 가장 많은 회원 조회
    * */
    @Test
    public void subQuery() throws Exception {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);

    }

    /**
     * 나이가 평균 이상인 회원 조회
     * */
    @Test
    public void subQueryGoe() throws Exception {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30, 40);

    }



    /**
     * 나이가 10살 이상인 회원 조회
     * */
    @Test
    public void subQueryIn() throws Exception {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);

    }

    @Test
    public void selectSubquery() throws Exception {

        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }
    @DisplayName("간단한 케이스문")
    @Test
    public void basicCase() throws Exception {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타")
                ).from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @DisplayName("복잡한 케이스문")
    @Test
    public void complexCase() throws Exception {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }

    }

    /**
     * 상수 추가 하기 */
    @Test
    public void constant() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 문자 더하기
     */
    @Test
    public void concat() throws Exception {


        //{username}_{age}
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     *프로젝션 셀렉 대상이 하나일 때
     */

    @Test
    public void simpleProjection() throws Exception {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
    /**
     *튜플 프로젝션
     */

    @Test
    public void tupleProjection() throws Exception {

        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    /**
     * JPQL의 프로젝션 DTO 반환 방법
     *
     * */
   @Test
    public void findDtoByJPQL() throws Exception {
        //given
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age)  from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * Querydsl 프로젝션 DTO 반환 방법 setter
     * */
    @Test
    public void findDtoBySetter() throws Exception {
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

    }

    /**
     * Querydsl 프로젝션 DTO 반환 방법 field
     * private field도 reflecton으로 접근하여 접근이 가능하다.
     * */
    @Test
    public void findDtoByField() throws Exception {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

    }

    /**
     * Querydsl 프로젝션 DTO 반환 방법 생성자
     * 매개변수 순서를 맞춰 줘야한다!
     * */
    @Test
    public void findDtoByConstructor() throws Exception {
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

    }

    /**
     * Querydsl 프로젝션 DTO 반환 방법 field
     * private field도 reflecton으로 접근하여 접근이 가능하다.
     * Dto에 담을 필드랑 from절의 필드랑 일치 하지 않을때는 as를 써서 해결한다
     * select절에 서브 쿼리를 날리고 싶다면
     * ExpressionUtils 과 JPAExpressions를 이용해서 해결
     * */
    @Test
    public void findUserDtoByField() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),

                        ExpressionUtils.as(JPAExpressions
                        .select(memberSub.age.max())
                        .from(memberSub), "age")
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }

    }

    /**
     * Querydsl 프로젝션 DTO 반환 방법 생성자
     * 매개변수 순서를 맞춰 줘야한다!
     * 필드의 이름이 달라도 순서로 들어가기 때문에 타입만 맞으면 된다.
     * */
    @Test
    public void findUserDtoByConstructor() throws Exception {
        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }

    }

    /**
     * Dto의 생성자에 @QueryProjection 를 쓰는 방법
     * Querydsl compile 필요
     * 컴파일시 생성자 타입체크 가능
     * 단점은 Dto까지 Q파일이 생성 된다는 것과 Dto가 Querydsl에 종속적이게 된다. */
    @Test
    public void findDtoByQueryProjection() throws Exception {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

    }

    /**
     * BooleanBuilder 동적 쿼리 */
    @Test
    public void dynamicQuery_BooleanBuilder() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = null;
        List<Member> result = serchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);

    }

    private List<Member> serchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }


        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }
    /**
     * Where안에 메서드를 만들어서 파라메터가 null이면 null 리턴 아니면
     * 조건문 넣고, where에 null이 들어가면 무시한다는 특징을 이용
     * BooleanExpression 을 이용해 메서드를 조합 할 수도 있다. compotison ! 재사용성이 좋다.
     */
    @Test
    public void dynaminQuery_WhereParam() throws Exception {
        //given
        String usernameParam = "member2";
        Integer ageParam = null;

        //when
        List<Member> result = serchMember2(usernameParam, ageParam);

        //then
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> serchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
//                .where(allEq(usernameCond, ageCond))
                .fetch();
    }

    private Predicate usernameEq(String usernameCond) {
//    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null ;
    }

    private Predicate ageEq(Integer ageCond) {
//    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

//    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
//        return usernameEq(usernameCond).and(ageEq(ageCond));
//    }

    @Test
    public void bulkUpdate() throws Exception {
        //given
        //영속성 컨텍스트의 상태는
        // member1 = 10 -> DB member1
        // member2 = 20 -> DB member1
        // member3 = 30 -> DB member3
        // member4 = 40 -> DB member4



        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        em.flush();
        em.clear();

        // bulk 연산은 1차캐시를 무시하고 바로 DB에 반영 하기 때문에 영속성컨텍스트의 객체와 DB의 값의 상태가 다르다.
        // DB와 1차캐시 중 1차 캐시를 우선시 하기 때문에 바뀌기전의 값이 나오게 된다.
        // 그래서 영속성컨텍스틀 플러쉬 하고 깨끗하기 초기화 시킨다.
        // 1 member1 = 10 ->1  DB 비회원 영속성 컨텍스트 member1
        // 2 member2 = 20 ->2  DB 비회원 영속성 컨텍스트 member2
        // 3 member3 = 30 ->3  DB member3 영속성 컨텍스트 member3
        // 4 member4 = 40 ->4  DB member4 영속성 컨텍스트 member4

        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member member : result) {
            System.out.println("member = " + member);
        }
    }


    @Test
    public void bulkAdd() throws Exception {
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.multiply(2))
                .execute();

    }

    @Test
    public void bulkDelete() {
        queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    @Test
    public void sqlFunction() throws Exception {
        List<String> result = queryFactory
                .select(
                        Expressions.stringTemplate(
                                "function('replace', {0}, {1}, {2})",
                                member.username, "member", "M"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 일반적인 DB에서 공통적으로 제공하는 ANSI 표준 함수들은 대부분 querydsl이 내장하고 있다고 한다.*/
    @Test
    public void sqlFunction2() throws Exception {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
//                .where(member.username.eq(Expressions.stringTemplate("function('lower', {0})", member.username)))
                .where(member.username.eq(member.username.lower()))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }



}
