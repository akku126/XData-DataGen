delete from prereq
delete from takes
delete from teaches
delete from section
delete from course
delete from advisor
delete from instructor
delete from student
delete from department
delete from classroom
insert into classroom values ('Taylor','3128','10')
insert into classroom values ('Taylor','3128','10')
insert into department values ('Music','Taylor','53564.9995')
insert into department values ('Physics','Painter',null)
insert into department values ('Finance','Taylor','53565.9995')
insert into department values ('Finance','Taylor','53565.9995')
insert into department values ('Finance','Taylor','53565.9995')
insert into department values ('Comp. Sci.','Packard',null)
insert into department values ('History','Packard','65009.9995')
insert into department values ('Physics','Painter',null)
insert into instructor values ('12121','Einstein',null,'49878.0')
insert into instructor values ('10101','Einstein','Finance','40000.0')
insert into instructor values ('12121','Einstein',null,'49878.0')
insert into instructor values ('10101','Einstein','Finance','40000.0')
insert into course values ('BIO-301','Music Video Production',null,'3')
insert into course values ('CS-347','Robotics','Physics','4')
insert into course values ('BIO-301','Music Video Production',null,'3')
insert into course values ('CS-347','Robotics','Physics','4')
insert into section values ('BIO-301','2','Summer','2010','Taylor','3128','B')
insert into section values ('CS-347','2','Summer','2009','Taylor',null,'C')
insert into teaches values ('12121','BIO-301','2','Summer','2010')
insert into teaches values ('10101','CS-347','2','Summer','2009')
