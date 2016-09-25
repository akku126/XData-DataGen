package testDataGen;

public class Experiments {

	public static void main(String[] args) throws Exception{
		String filePath="4";
		GenerateDataset_new g=new GenerateDataset_new(filePath);
		
		
		/**Time for Tuple assignment Experiments*/
		//g.generateDatasetForQuery("1", "true", "SELECT c.dept_name, SUM(c.credits) FROM course c INNER JOIN department d ON (c.dept_name = d.dept_name)  GROUP BY c.dept_name  HAVING SUM(c.credits) > 10 AND COUNT(c.credits) > 1", "");
		//g.generateDatasetForQuery("2", "true", "SELECT c.dept_name, SUM(i.salary) FROM course c INNER JOIN department d ON (c.dept_name = d.dept_name) INNER JOIN instructor i ON (d.dept_name = i.dept_name) GROUP BY c.dept_name HAVING SUM(i.salary) > 100000 AND MAX(i.salary) < 75000", "");
		//g.generateDatasetForQuery("3", "true", "SELECT c.dept_name, SUM(d.budget) FROM course c INNER JOIN department d ON (c.dept_name = d.dept_name) INNER JOIN teaches t ON (c.course_id = t.course_id) GROUP BY c.dept_name HAVING SUM(d.budget) > 100000 AND COUNT(d.budget) > 1", "");
		//g.generateDatasetForQuery("4", "true", "SELECT c.dept_name, AVG(i.salary) FROM course c INNER JOIN department d ON (c.dept_name = d.dept_name) INNER JOIN teaches t ON (c.course_id = t.course_id) INNER JOIN instructor i ON (d.dept_name = i.dept_name) GROUP BY c.dept_name HAVING AVG(i.salary) > 50000 AND COUNT(i.salary) = 3", "");
		//FIXME:IN below query Error due to group by attribute not present: 
		//g.generateDatasetForQuery("5", "true", "SELECT t.semester, SUM(c.credits) FROM department d INNER JOIN teaches t  ON (d.budget = t.year + 4)  INNER JOIN course c ON (c.dept_name = d.dept_name)  GROUP BY t.semester HAVING AVG(c.credits) > 2 AND COUNT(d.building) = 2", "");
		//g.generateDatasetForQuery("6", "true", "SELECT id FROM course NATURAL JOIN department NATURAL JOIN student NATURAL JOIN takes NATURAL JOIN section   GROUP BY id,dept_name  HAVING COUNT(dept_name)>1", "");
		//g.generateDatasetForQuery("7", "true", "SELECT distinct dept_name FROM course WHERE credits =(SELECT MAX(credits) FROM course NATURAL JOIN DEPARTMENT WHERE  title='CS' GROUP BY dept_name HAVING COUNT(course_id)> 2)", "");
		//g.generateDatasetForQuery("8", "true", "SELECT id,name FROM (SELECT id,time_slot_id,year,semester FROM takes NATURAL JOIN section GROUP BY id,time_slot_id,year,semester HAVING COUNT(time_slot_id)>1) as s NATURAL JOIN student GROUP BY id, name HAVING COUNT(id)>1", "");
		//g.generateDatasetForQuery("9", "true", "SELECT SUM(T) as su FROM (SELECT year as T FROM teaches  NATURAL JOIN instructor GROUP BY year, course_id HAVING COUNT(id)>4 ) as temp GROUP BY T ", "");
		
		/**experiments for data generation*/
		//g.generateDatasetForQuery("1", "true", "", "");
		//g.generateDatasetForQuery("2", "true", "", "");
		//g.generateDatasetForQuery("3", "true", "", "");
		//g.generateDatasetForQuery("4", "true", "", "");
		//g.generateDatasetForQuery("5", "true", "", "");
		//g.generateDatasetForQuery("6", "true", "", "");
		//g.generateDatasetForQuery("7", "true", "", "");
		//g.generateDatasetForQuery("8", "true", "", "");
		//g.generateDatasetForQuery("9", "true", "", "");
		//g.generateDatasetForQuery("10", "true", "", "");
		//g.generateDatasetForQuery("11", "true", "", "");
		//g.generateDatasetForQuery("12", "true", "", "");
		//g.generateDatasetForQuery("13", "true", "", "");
		//g.generateDatasetForQuery("14", "true", "", "");
		//g.generateDatasetForQuery("15", "true", "", "");
		
		
		
		/**Mutation Killing*/
		//g.generateDatasetForQuery("M1", "true", "SELECT * FROM (SELECT id,time_slot_id,year,semester FROM takes NATURAL JOIN section GROUP BY id,time_slot_id,year,semester HAVING COUNT(time_slot_id)>1) as s", "");
		//g.generateDatasetForQuery("M2", "true", "SELECT distinct dept_name FROM course WHERE credits IN (SELECT SUM(credits) FROM course NATURAL JOIN department WHERE  title='CS' GROUP BY dept_name, building HAVING COUNT(course_id)> 2)", "");
		//g.generateDatasetForQuery("M3", "true", "SELECT COUNT(course_id) FROM (SELECT id,time_slot_id,year,semester FROM takes NATURAL JOIN section GROUP BY id,time_slot_id,year,semester HAVING COUNT(time_slot_id)>1) as s NATURAL JOIN course WHERE credits =(SELECT MAX(credits) FROM course NATURAL JOIN department WHERE  title='CS' GROUP BY dept_name, building HAVING COUNT(course_id)> 2) GROUP BY id,dept_name HAVING COUNT(title)>1", "");
		

	}
}
