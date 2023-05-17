

;------------------------------------------------------------
;MUTATION TYPE: DATASET TO KILL COLUMN REPLACEMENT IN PROJECTION 
;------------------------------------------------------------
(set-option :produce-models true)
 (set-option :smt.macro_finder true) 

(declare-fun intNullVal () Int)
(assert (= intNullVal (- 99996)))

(declare-fun realNullVal () Real)
(assert (= realNullVal (- 99996.0)))

(declare-datatypes ((ID 0)) (((_ID__10101) (_ID__12121) (_ID__15151) (_ID__22222) (_ID__32343) (_ID__33456) (_ID__45565) (_ID__58583) (_ID__76543) (_ID__76766) (_ID__83821) (_ID__98345) (_ID__V13) (_ID__V14) (_ID__V15) (_ID__V16) (_ID__V17) (_ID__V18) (_ID__V19) (_ID__V20) (NULL_ID_1))))

(declare-fun ISNULL_ID (ID) Bool)
(assert (forall ((id ID)) (= (ISNULL_ID id) (or (= id NULL_ID_1)))))




(declare-datatypes ((NAME 0)) (((_NAME__Einstein) (_NAME__Katz) (_NAME__Singh) (_NAME__Kim) (_NAME__Brandt) (_NAME__El_ubSaid) (_NAME__Wu) (_NAME__Srinivasan) (_NAME__Crick) (_NAME__Gold) (_NAME__Califieri) (_NAME__Mozart) (_NAME__V13) (_NAME__V14) (_NAME__V15) (_NAME__V16) (_NAME__V17) (_NAME__V18) (_NAME__V19) (_NAME__V20) (NULL_NAME_1))))

(declare-fun ISNULL_NAME (NAME) Bool)
(assert (forall ((name NAME)) (= (ISNULL_NAME name) (or (= name NULL_NAME_1)))))




(declare-datatypes ((DEPT_NAME 0)) (((_DEPT_uNAME__Finance) (_DEPT_uNAME__History) (_DEPT_uNAME__Physics) (_DEPT_uNAME__Music) (_DEPT_uNAME__Comp_d_ubSci_d) (_DEPT_uNAME__Biology) (_DEPT_uNAME__Elec_d_ubEng_d) (_DEPT_uNAME__V8) (_DEPT_uNAME__V9) (_DEPT_uNAME__V10) (_DEPT_uNAME__V11) (_DEPT_uNAME__V12) (_DEPT_uNAME__V13) (_DEPT_uNAME__V14) (_DEPT_uNAME__V15) (_DEPT_uNAME__V16) (_DEPT_uNAME__V17) (_DEPT_uNAME__V18) (_DEPT_uNAME__V19) (_DEPT_uNAME__V20) (NULL_DEPT_NAME_1))))

(declare-fun ISNULL_DEPT_NAME (DEPT_NAME) Bool)
(assert (forall ((dept_name DEPT_NAME))
  (= (ISNULL_DEPT_NAME dept_name) (or (= dept_name NULL_DEPT_NAME_1)))))




(declare-fun checkSALARY (Real) Bool)
(assert (forall ((r_SALARY Real))
  (= (checkSALARY r_SALARY)
     (or (and (>= r_SALARY 40000.0) (<= r_SALARY 95000.0))
         (= r_SALARY (- 99996.0))))))

(declare-fun ISNULL_SALARY (Real) Bool)
(assert (forall ((salary Real)) (= (ISNULL_SALARY salary) (or (= salary realNullVal)))))





(declare-datatypes ((COURSE_ID 0)) (((_COURSE_uID__BIO_m301) (_COURSE_uID__CS_m347) (_COURSE_uID__FIN_m201) (_COURSE_uID__CS_m315) (_COURSE_uID__EE_m181) (_COURSE_uID__BIO_m101) (_COURSE_uID__MU_m199) (_COURSE_uID__HIS_m351) (_COURSE_uID__PHY_m101) (_COURSE_uID__CS_m319) (_COURSE_uID__CS_m101) (_COURSE_uID__CS_m190) (_COURSE_uID__V13) (_COURSE_uID__V14) (_COURSE_uID__V15) (_COURSE_uID__V16) (_COURSE_uID__V17) (_COURSE_uID__V18) (_COURSE_uID__V19) (_COURSE_uID__V20) (NULL_COURSE_ID_1))))

(declare-fun ISNULL_COURSE_ID (COURSE_ID) Bool)
(assert (forall ((course_id COURSE_ID))
  (= (ISNULL_COURSE_ID course_id) (or (= course_id NULL_COURSE_ID_1)))))




(declare-datatypes ((SEC_ID 0)) (((_SEC_uID__2) (_SEC_uID__1) (_SEC_uID__V3) (_SEC_uID__V4) (_SEC_uID__V5) (_SEC_uID__V6) (_SEC_uID__V7) (_SEC_uID__V8) (_SEC_uID__V9) (_SEC_uID__V10) (_SEC_uID__V11) (_SEC_uID__V12) (_SEC_uID__V13) (_SEC_uID__V14) (_SEC_uID__V15) (_SEC_uID__V16) (_SEC_uID__V17) (_SEC_uID__V18) (_SEC_uID__V19) (_SEC_uID__V20) (NULL_SEC_ID_1))))

(declare-fun ISNULL_SEC_ID (SEC_ID) Bool)
(assert (forall ((sec_id SEC_ID))
  (= (ISNULL_SEC_ID sec_id) (or (= sec_id NULL_SEC_ID_1)))))




(declare-datatypes ((SEMESTER 0)) (((_SEMESTER__Summer) (_SEMESTER__Spring) (_SEMESTER__Fall) (_SEMESTER__V4) (_SEMESTER__V5) (_SEMESTER__V6) (_SEMESTER__V7) (_SEMESTER__V8) (_SEMESTER__V9) (_SEMESTER__V10) (_SEMESTER__V11) (_SEMESTER__V12) (_SEMESTER__V13) (_SEMESTER__V14) (_SEMESTER__V15) (_SEMESTER__V16) (_SEMESTER__V17) (_SEMESTER__V18) (_SEMESTER__V19) (_SEMESTER__V20) (NULL_SEMESTER_1))))

(declare-fun ISNULL_SEMESTER (SEMESTER) Bool)
(assert (forall ((semester SEMESTER))
  (= (ISNULL_SEMESTER semester) (or (= semester NULL_SEMESTER_1)))))




(declare-fun checkYEAR (Int) Bool)
(assert (forall ((i_YEAR Int))
  (= (checkYEAR i_YEAR)
     (or (and (> i_YEAR 2008) (< i_YEAR 2011)) (= i_YEAR (- 99996))))))

(declare-fun ISNULL_YEAR (Int) Bool)
(assert (forall ((year Int)) (= (ISNULL_YEAR year) (or (= year intNullVal)))))





(declare-datatypes ((BUILDING 0)) (((_BUILDING__Taylor) (_BUILDING__Packard) (_BUILDING__Painter) (_BUILDING__Watson) (_BUILDING__V5) (_BUILDING__V6) (_BUILDING__V7) (_BUILDING__V8) (_BUILDING__V9) (_BUILDING__V10) (_BUILDING__V11) (_BUILDING__V12) (_BUILDING__V13) (_BUILDING__V14) (_BUILDING__V15) (_BUILDING__V16) (_BUILDING__V17) (_BUILDING__V18) (_BUILDING__V19) (_BUILDING__V20) (NULL_BUILDING_1))))

(declare-fun ISNULL_BUILDING (BUILDING) Bool)
(assert (forall ((building BUILDING))
  (= (ISNULL_BUILDING building) (or (= building NULL_BUILDING_1)))))




(declare-fun checkBUDGET (Real) Bool)
(assert (forall ((r_BUDGET Real))
  (= (checkBUDGET r_BUDGET)
     (or (and (>= r_BUDGET 50000.0) (<= r_BUDGET 120000.0))
         (= r_BUDGET (- 99996.0))))))

(declare-fun ISNULL_BUDGET (Real) Bool)
(assert (forall ((budget Real)) (= (ISNULL_BUDGET budget) (or (= budget realNullVal)))))





(declare-datatypes ((ROOM_NUMBER 0)) (((_ROOM_uNUMBER__3128) (_ROOM_uNUMBER__120) (_ROOM_uNUMBER__100) (_ROOM_uNUMBER__101) (_ROOM_uNUMBER__514) (_ROOM_uNUMBER__V6) (_ROOM_uNUMBER__V7) (_ROOM_uNUMBER__V8) (_ROOM_uNUMBER__V9) (_ROOM_uNUMBER__V10) (_ROOM_uNUMBER__V11) (_ROOM_uNUMBER__V12) (_ROOM_uNUMBER__V13) (_ROOM_uNUMBER__V14) (_ROOM_uNUMBER__V15) (_ROOM_uNUMBER__V16) (_ROOM_uNUMBER__V17) (_ROOM_uNUMBER__V18) (_ROOM_uNUMBER__V19) (_ROOM_uNUMBER__V20) (NULL_ROOM_NUMBER_1))))

(declare-fun ISNULL_ROOM_NUMBER (ROOM_NUMBER) Bool)
(assert (forall ((room_number ROOM_NUMBER))
  (= (ISNULL_ROOM_NUMBER room_number) (or (= room_number NULL_ROOM_NUMBER_1)))))




(declare-datatypes ((TIME_SLOT_ID 0)) (((_TIME_uSLOT_uID__B) (_TIME_uSLOT_uID__C) (_TIME_uSLOT_uID__H) (_TIME_uSLOT_uID__D) (_TIME_uSLOT_uID__E) (_TIME_uSLOT_uID__F) (_TIME_uSLOT_uID__A) (_TIME_uSLOT_uID__V8) (_TIME_uSLOT_uID__V9) (_TIME_uSLOT_uID__V10) (_TIME_uSLOT_uID__V11) (_TIME_uSLOT_uID__V12) (_TIME_uSLOT_uID__V13) (_TIME_uSLOT_uID__V14) (_TIME_uSLOT_uID__V15) (_TIME_uSLOT_uID__V16) (_TIME_uSLOT_uID__V17) (_TIME_uSLOT_uID__V18) (_TIME_uSLOT_uID__V19) (_TIME_uSLOT_uID__V20) (NULL_TIME_SLOT_ID_1))))

(declare-fun ISNULL_TIME_SLOT_ID (TIME_SLOT_ID) Bool)
(assert (forall ((time_slot_id TIME_SLOT_ID))
  (= (ISNULL_TIME_SLOT_ID time_slot_id)
     (or (= time_slot_id NULL_TIME_SLOT_ID_1)))))




(declare-datatypes ((TITLE 0)) (((_TITLE__Music_ubVideo_ubProduction) (_TITLE__Robotics) (_TITLE__Game_ubDesign) (_TITLE__Intro_d_ubto_ubComputer_ubScience) (_TITLE__Database_ubSystem_ubConcepts) (_TITLE__Computational_ubBiology) (_TITLE__Genetics) (_TITLE__Intro_d_ubto_ubBiology) (_TITLE__World_ubHistory) (_TITLE__Intro_d_ubto_ubDigital_ubSystems) (_TITLE__Physical_ubPrinciples) (_TITLE__Investment_ubBanking) (_TITLE__Image_ubProcessing) (_TITLE__V14) (_TITLE__V15) (_TITLE__V16) (_TITLE__V17) (_TITLE__V18) (_TITLE__V19) (_TITLE__V20) (NULL_TITLE_1))))

(declare-fun ISNULL_TITLE (TITLE) Bool)
(assert (forall ((title TITLE)) (= (ISNULL_TITLE title) (or (= title NULL_TITLE_1)))))




(declare-fun checkCREDITS (Int) Bool)
(assert (forall ((i_CREDITS Int))
  (= (checkCREDITS i_CREDITS)
     (or (and (> i_CREDITS 2) (< i_CREDITS 5)) (= i_CREDITS (- 99996))))))

(declare-fun ISNULL_CREDITS (Int) Bool)
(assert (forall ((credits Int))
  (= (ISNULL_CREDITS credits) (or (= credits intNullVal)))))





(declare-fun checkCAPACITY (Int) Bool)
(assert (forall ((i_CAPACITY Int))
  (= (checkCAPACITY i_CAPACITY)
     (or (and (> i_CAPACITY 9) (< i_CAPACITY 501)) (= i_CAPACITY (- 99996))))))

(declare-fun ISNULL_CAPACITY (Int) Bool)
(assert (forall ((capacity Int))
  (= (ISNULL_CAPACITY capacity) (or (= capacity intNullVal)))))







;------------------------------------------------------------
; Tuple Types for Relations
 
;------------------------------------------------------------
(declare-datatypes ((teaches_TupleType 0)) (((teaches_TupleType (teaches_ID0 ID) (teaches_COURSE_ID1 COURSE_ID) (teaches_SEC_ID2 SEC_ID) (teaches_SEMESTER3 SEMESTER) (teaches_YEAR4 Int)))))

(declare-datatypes ((section_TupleType 0)) (((section_TupleType (section_COURSE_ID0 COURSE_ID) (section_SEC_ID1 SEC_ID) (section_SEMESTER2 SEMESTER) (section_YEAR3 Int) (section_BUILDING4 BUILDING) (section_ROOM_NUMBER5 ROOM_NUMBER) (section_TIME_SLOT_ID6 TIME_SLOT_ID)))))

(declare-datatypes ((department_TupleType 0)) (((department_TupleType (department_DEPT_NAME0 DEPT_NAME) (department_BUILDING1 BUILDING) (department_BUDGET2 Real)))))

(declare-datatypes ((course_TupleType 0)) (((course_TupleType (course_COURSE_ID0 COURSE_ID) (course_TITLE1 TITLE) (course_DEPT_NAME2 DEPT_NAME) (course_CREDITS3 Int)))))

(declare-datatypes ((instructor_TupleType 0)) (((instructor_TupleType (instructor_ID0 ID) (instructor_NAME1 NAME) (instructor_DEPT_NAME2 DEPT_NAME) (instructor_SALARY3 Real)))))

(declare-datatypes ((classroom_TupleType 0)) (((classroom_TupleType (classroom_BUILDING0 BUILDING) (classroom_ROOM_NUMBER1 ROOM_NUMBER) (classroom_CAPACITY2 Int)))))

(declare-fun O_instructor () (Array Int instructor_TupleType))

(declare-fun O_teaches () (Array Int teaches_TupleType))

(declare-fun O_department () (Array Int department_TupleType))

(declare-fun O_section () (Array Int section_TupleType))

(declare-fun O_course () (Array Int course_TupleType))

(declare-fun O_classroom () (Array Int classroom_TupleType))

;------------------------------------------------------------
;FOREIGN KEY CONSTRAINTS
;------------------------------------------------------------
(assert (or (= (teaches_ID0 (select O_teaches 1))
       (instructor_ID0 (select O_instructor 2)))))
(assert (or (= (teaches_COURSE_ID1 (select O_teaches 1))
       (section_COURSE_ID0 (select O_section 1)))))

(assert (or (= (teaches_SEC_ID2 (select O_teaches 1))
       (section_SEC_ID1 (select O_section 1)))))

(assert (or (= (teaches_SEMESTER3 (select O_teaches 1))
       (section_SEMESTER2 (select O_section 1)))))

(assert (or (= (teaches_YEAR4 (select O_teaches 1))
       (section_YEAR3 (select O_section 1)))))
(assert (or (= (teaches_COURSE_ID1 (select O_teaches 1))
       (course_COURSE_ID0 (select O_course 1)))))
(assert (or (= (section_BUILDING4 (select O_section 1))
       (classroom_BUILDING0 (select O_classroom 1)))
    (ISNULL_BUILDING (section_BUILDING4 (select O_section 1)))))

(assert (or (= (section_ROOM_NUMBER5 (select O_section 1))
       (classroom_ROOM_NUMBER1 (select O_classroom 1)))
    (ISNULL_ROOM_NUMBER (section_ROOM_NUMBER5 (select O_section 1)))))
(assert (or (= (section_COURSE_ID0 (select O_section 1))
       (course_COURSE_ID0 (select O_course 2)))))
(assert (or (= (course_DEPT_NAME2 (select O_course 1))
       (department_DEPT_NAME0 (select O_department 1)))
    (ISNULL_DEPT_NAME (course_DEPT_NAME2 (select O_course 1)))))

(assert (or (= (course_DEPT_NAME2 (select O_course 2))
       (department_DEPT_NAME0 (select O_department 2)))
    (ISNULL_DEPT_NAME (course_DEPT_NAME2 (select O_course 2)))))
(assert (or (= (instructor_DEPT_NAME2 (select O_instructor 1))
       (department_DEPT_NAME0 (select O_department 3)))
    (ISNULL_DEPT_NAME (instructor_DEPT_NAME2 (select O_instructor 1)))))

(assert (or (= (instructor_DEPT_NAME2 (select O_instructor 2))
       (department_DEPT_NAME0 (select O_department 4)))
    (ISNULL_DEPT_NAME (instructor_DEPT_NAME2 (select O_instructor 2)))))


;------------------------------------------------------------
;END OF FOREIGN  KEY CONSTRAINTS
;------------------------------------------------------------


;------------------------------------------------------------
;DOMAIN CONSTRAINTS
;------------------------------------------------------------
(assert (forall ((i Int))
  (let ((a!1 (or (checkSALARY (instructor_SALARY3 (select O_instructor i)))
                 (ISNULL_SALARY (instructor_SALARY3 (select O_instructor i))))))
    (=> (and (<= 1 i) (<= i 2)) (and a!1)))))

(assert (forall ((i Int))
  (let ((a!1 (or (checkYEAR (teaches_YEAR4 (select O_teaches i)))
                 (ISNULL_YEAR (teaches_YEAR4 (select O_teaches i))))))
    (=> (and (<= 1 i) (<= i 1)) (and a!1)))))

(assert (forall ((i Int))
  (let ((a!1 (or (checkBUDGET (department_BUDGET2 (select O_department i)))
                 (ISNULL_BUDGET (department_BUDGET2 (select O_department i))))))
    (=> (and (<= 1 i) (<= i 4)) (and a!1)))))

(assert (forall ((i Int))
  (let ((a!1 (or (checkYEAR (section_YEAR3 (select O_section i)))
                 (ISNULL_YEAR (section_YEAR3 (select O_section i))))))
    (=> (and (<= 1 i) (<= i 1)) (and a!1)))))

(assert (forall ((i Int))
  (let ((a!1 (or (checkCREDITS (course_CREDITS3 (select O_course i)))
                 (ISNULL_CREDITS (course_CREDITS3 (select O_course i))))))
    (=> (and (<= 1 i) (<= i 2)) (and a!1)))))

(assert (forall ((i Int))
  (let ((a!1 (or (checkCAPACITY (classroom_CAPACITY2 (select O_classroom i)))
                 (ISNULL_CAPACITY (classroom_CAPACITY2 (select O_classroom i))))))
    (=> (and (<= 1 i) (<= i 1)) (and a!1)))))

;------------------------------------------------------------
;END OF DOMAIN CONSTRAINTS
;------------------------------------------------------------

(declare-fun CHECKALL_NULLReal (Real) Bool)
(assert (forall ((Realcol Real))
  (= (CHECKALL_NULLReal Realcol) (= Realcol realNullVal))))

(declare-fun MAX_REPLACE_NULL_Real (Real) Real)
(assert (forall ((Realcol Real))
  (= (MAX_REPLACE_NULL_Real Realcol)
     (ite (= Realcol realNullVal) realNullVal Realcol))))

(declare-fun SUM_REPLACE_NULL_Real (Real) Real)
(assert (forall ((Realcol Real))
  (= (SUM_REPLACE_NULL_Real Realcol) (ite (= Realcol realNullVal) 0.0 Realcol))))

(declare-fun MIN_REPLACE_NULL_Real (Real) Real)
(assert (forall ((Realcol Real))
  (= (MIN_REPLACE_NULL_Real Realcol)
     (ite (= Realcol realNullVal) (- 0.0 realNullVal) Realcol))))

(declare-fun CHECKALL_NULLInt (Int) Bool)
(assert (forall ((Intcol Int)) (= (CHECKALL_NULLInt Intcol) (= Intcol intNullVal))))

(declare-fun MAX_REPLACE_NULL_Int (Int) Int)
(assert (forall ((Intcol Int))
  (= (MAX_REPLACE_NULL_Int Intcol)
     (ite (= Intcol intNullVal) intNullVal Intcol))))

(declare-fun SUM_REPLACE_NULL_Int (Int) Int)
(assert (forall ((Intcol Int))
  (= (SUM_REPLACE_NULL_Int Intcol) (ite (= Intcol intNullVal) 0 Intcol))))

(declare-fun MIN_REPLACE_NULL_Int (Int) Int)
(assert (forall ((Intcol Int))
  (= (MIN_REPLACE_NULL_Int Intcol)
     (ite (= Intcol intNullVal) (- 0 intNullVal) Intcol))))


;------------------------------------------------------------
;PRIMARY KEY CONSTRAINTS
;------------------------------------------------------------
(assert (=> (= (instructor_ID0 (select O_instructor 1))(instructor_ID0 (select O_instructor 2)) )(and (= (instructor_NAME1 (select O_instructor 1))(instructor_NAME1 (select O_instructor 2)) )(= (instructor_DEPT_NAME2 (select O_instructor 1))(instructor_DEPT_NAME2 (select O_instructor 2)) )(= (instructor_SALARY3 (select O_instructor 1))(instructor_SALARY3 (select O_instructor 2)) )) )) 

(assert (=> (= (department_DEPT_NAME0 (select O_department 1))(department_DEPT_NAME0 (select O_department 2)) )(and (= (department_BUILDING1 (select O_department 1))(department_BUILDING1 (select O_department 2)) )(= (department_BUDGET2 (select O_department 1))(department_BUDGET2 (select O_department 2)) )) )) 

(assert (=> (= (department_DEPT_NAME0 (select O_department 1))(department_DEPT_NAME0 (select O_department 3)) )(and (= (department_BUILDING1 (select O_department 1))(department_BUILDING1 (select O_department 3)) )(= (department_BUDGET2 (select O_department 1))(department_BUDGET2 (select O_department 3)) )) )) 

(assert (=> (= (department_DEPT_NAME0 (select O_department 1))(department_DEPT_NAME0 (select O_department 4)) )(and (= (department_BUILDING1 (select O_department 1))(department_BUILDING1 (select O_department 4)) )(= (department_BUDGET2 (select O_department 1))(department_BUDGET2 (select O_department 4)) )) )) 

(assert (=> (= (department_DEPT_NAME0 (select O_department 2))(department_DEPT_NAME0 (select O_department 3)) )(and (= (department_BUILDING1 (select O_department 2))(department_BUILDING1 (select O_department 3)) )(= (department_BUDGET2 (select O_department 2))(department_BUDGET2 (select O_department 3)) )) )) 

(assert (=> (= (department_DEPT_NAME0 (select O_department 2))(department_DEPT_NAME0 (select O_department 4)) )(and (= (department_BUILDING1 (select O_department 2))(department_BUILDING1 (select O_department 4)) )(= (department_BUDGET2 (select O_department 2))(department_BUDGET2 (select O_department 4)) )) )) 

(assert (=> (= (department_DEPT_NAME0 (select O_department 3))(department_DEPT_NAME0 (select O_department 4)) )(and (= (department_BUILDING1 (select O_department 3))(department_BUILDING1 (select O_department 4)) )(= (department_BUDGET2 (select O_department 3))(department_BUDGET2 (select O_department 4)) )) )) 

(assert (=> (= (course_COURSE_ID0 (select O_course 1))(course_COURSE_ID0 (select O_course 2)) )(and (= (course_TITLE1 (select O_course 1))(course_TITLE1 (select O_course 2)) )(= (course_DEPT_NAME2 (select O_course 1))(course_DEPT_NAME2 (select O_course 2)) )(= (course_CREDITS3 (select O_course 1))(course_CREDITS3 (select O_course 2)) )) )) 



;------------------------------------------------------------
;END OF PRIMARY KEY CONSTRAINTS
;------------------------------------------------------------


;------------------------------------------------------------
;UNIQUE CONSTRAINTS FOR PRIMARY KEY TO SATISFY CONSTRAINED AGGREGATION
;------------------------------------------------------------


;------------------------------------------------------------
;END OF UNIQUE CONSTRAINTS  FOR PRIMARY KEY TO SATISFY CONSTRAINED AGGREGATION
;------------------------------------------------------------




;------------------------------------------------------------
; CONSTRAINTS OF THIS BLOCK 
;------------------------------------------------------------

 (assert  (and (= (instructor_ID0 (select O_instructor 1)) (teaches_ID0 (select O_teaches 1)))	)) 


;------------------------------------------------------------
; CONSTRAINTS FOR THIS CONJUNCT 
;------------------------------------------------------------


;------------------------------------------------------------
; WHERE CLAUSE SUBQUERY BLOCK CONSTRAINTS 
;------------------------------------------------------------


;------------------------------------------------------------
; END OF WHERE CLAUSE SUBQUERY BLOCK CONSTRAINTS 
;------------------------------------------------------------


;------------------------------------------------------------
;GROUP BY CLAUSE CONSTRAINTS
;------------------------------------------------------------


;------------------------------------------------------------
;END OF GROUP BY CLAUSE CONSTRAINTS
;------------------------------------------------------------


;------------------------------------------------------------
;HAVING CLAUSE CONSTRAINTS
;------------------------------------------------------------


;------------------------------------------------------------
;END OF HAVING CLAUSE CONSTRAINTS
;------------------------------------------------------------


;------------------------------------------------------------
;PARAMETERIZED CLAUSE CONSTRAINTS 
;------------------------------------------------------------


;------------------------------------------------------------
;END OF PARAMETERIZED CLAUSE CONSTRAINTS 
;------------------------------------------------------------


;------------------------------------------------------------
;APPLICATION CONSTRAINTS 
;------------------------------------------------------------


;------------------------------------------------------------
;END OF APPLICATION CONSTRAINTS 
;------------------------------------------------------------


;------------------------------------------------------------
; UNIQUE  KEY CONSTRAINTS 
;------------------------------------------------------------


;------------------------------------------------------------
;END OF UNIQUE  KEY CONSTRAINTS 
;------------------------------------------------------------


;------------------------------------------------------------
; END OF CONSTRAINTS OF THIS BLOCK 
;------------------------------------------------------------



;------------------------------------------------------------
; COLUMN REPLACEMENT
;------------------------------------------------------------



;------------------------------------------------------------
; END OF COLUMN REPLACEMENT
;------------------------------------------------------------



;------------------------------------------------------------
;NULL CONSTRAINTS FOR OUTER BLOCK OF QUERY
;------------------------------------------------------------




;------------------------------------------------------------
;END OF NULL CONSTRAINTS FOR OUTER BLOCK OF QUERY
;------------------------------------------------------------



;------------------------------------------------------------
;BRANCHQUERY CONSTRAINTS
;------------------------------------------------------------



;------------------------------------------------------------
; BRANCH QUERY CONSTRAINTS FOR DIFFERENT GROUP BY COLUMN FROM PRIMARY QUERY
;------------------------------------------------------------


;------------------------------------------------------------
; BRANCH QUERY CONSTRAINTS FOR DIFFERENT GROUP BY COLUMN FROM OTHER BRANCH QUERIES
;------------------------------------------------------------


;------------------------------------------------------------
; BRANCH QUERY CONSTRAINTS FOR SAME GROUP BY COLUMN OF A BRANCH QUERY
;------------------------------------------------------------


;------------------------------------------------------------
; BRANCH QUERY GROUP BY CONSTRAINTS
;------------------------------------------------------------


;------------------------------------------------------------
; PRIMARY KEY CONSTRAINTS FOR BRANCH QUERY
;------------------------------------------------------------


;------------------------------------------------------------
; FOREIGN KEY CONSTRAINTS FOR BRANCH QUERY
;------------------------------------------------------------


;------------------------------------------------------------
;NOT NULL CONSTRAINTS FOR BRANCHQUERY


;------------------------------------------------------------



;------------------------------------------------------------
;END OF BRANCHQUERY CONSTRAINTS
;------------------------------------------------------------


;------------------------------------------------------------
;NOT NULL CONSTRAINTS


;------------------------------------------------------------

(assert (forall ((ipk0 Int)) 
 (and 
	(not ( ISNULL_ID(instructor_ID0 (select O_instructor ipk0))) )
 )
))

(assert (forall ((ipk0 Int)) 
 (and 
	(not ( ISNULL_ID(teaches_ID0 (select O_teaches ipk0))) )
	(not ( ISNULL_COURSE_ID(teaches_COURSE_ID1 (select O_teaches ipk0))) )
	(not ( ISNULL_SEC_ID(teaches_SEC_ID2 (select O_teaches ipk0))) )
	(not ( ISNULL_SEMESTER(teaches_SEMESTER3 (select O_teaches ipk0))) )
	(not ( ISNULL_YEAR(teaches_YEAR4 (select O_teaches ipk0))) )
 )
))

(assert (forall ((ipk0 Int)) 
 (and 
	(not ( ISNULL_DEPT_NAME(department_DEPT_NAME0 (select O_department ipk0))) )
 )
))

(assert (forall ((ipk0 Int)) 
 (and 
	(not ( ISNULL_COURSE_ID(section_COURSE_ID0 (select O_section ipk0))) )
	(not ( ISNULL_SEC_ID(section_SEC_ID1 (select O_section ipk0))) )
	(not ( ISNULL_SEMESTER(section_SEMESTER2 (select O_section ipk0))) )
	(not ( ISNULL_YEAR(section_YEAR3 (select O_section ipk0))) )
 )
))

(assert (forall ((ipk0 Int)) 
 (and 
	(not ( ISNULL_COURSE_ID(course_COURSE_ID0 (select O_course ipk0))) )
 )
))

(assert (forall ((ipk0 Int)) 
 (and 
	(not ( ISNULL_BUILDING(classroom_BUILDING0 (select O_classroom ipk0))) )
	(not ( ISNULL_ROOM_NUMBER(classroom_ROOM_NUMBER1 (select O_classroom ipk0))) )
 )
))


;------------------------------------------------------------
;END OF NOT NULL CONSTRAINTS


;------------------------------------------------------------


(check-sat)
(get-model)