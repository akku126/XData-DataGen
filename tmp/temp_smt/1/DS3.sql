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
insert into classroom values ('Taylor','3128','123')
insert into classroom values ('Packard','120','10')
insert into department values ('Physics','Taylor','67059.0')
insert into department values ('Music','Painter','50000.0')
insert into department values ('History','Taylor','62572.0')
insert into department values ('Finance','Packard','108555.0')
insert into department values ('History','Taylor','62572.0')
insert into department values ('Physics','Taylor','67059.0')
insert into department values ('Music','Painter','50000.0')
insert into instructor values ('10101','Katz','History','40000.0')
insert into instructor values ('10101','Katz','History','40000.0')
insert into instructor values ('12121','Einstein','Finance','82454.5')
insert into course values ('CS-347','Music Video Production','Physics','4')
insert into course values ('BIO-301','Music Video Production','Music','3')
insert into course values ('CS-347','Music Video Production','Physics','4')
insert into course values ('BIO-301','Music Video Production','Music','3')
insert into section values ('CS-347','2','Spring','2010',null,'3128','B')
insert into section values ('BIO-301','1','Summer','2009',null,'120','B')
insert into teaches values ('12121','CS-347','2','Spring','2010')
insert into teaches values ('10101','BIO-301','1','Summer','2009')
