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
insert into classroom values ('Taylor','120','416')
insert into classroom values ('Packard','3128','10')
insert into department values ('Physics','Painter','65974.0')
insert into department values ('Finance','Taylor','53143.0')
insert into department values ('Music','Taylor','53142.0')
insert into department values ('Finance','Taylor','53143.0')
insert into department values ('History','Packard','54263.0')
insert into department values ('Physics','Painter','65974.0')
insert into department values ('Finance','Taylor','53143.0')
insert into instructor values ('12121','Einstein',null,'49878.0')
insert into instructor values ('10101','Einstein',null,'40000.0')
insert into instructor values ('15151','Katz','Finance','88702.5')
insert into course values ('BIO-301','Music Video Production','Physics','4')
insert into course values ('CS-347','Robotics','Finance','4')
insert into course values ('BIO-301','Music Video Production','Physics','4')
insert into course values ('CS-347','Robotics','Finance','4')
insert into section values ('BIO-301','2','Summer','2010','Taylor','120','B')
insert into section values ('CS-347','1','Spring','2009','Packard','3128','B')
insert into teaches values ('15151','BIO-301','2','Summer','2010')
insert into teaches values ('12121','CS-347','1','Spring','2009')
