package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import java.util.List;

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

}
