# Session Prompts - Project Scheduler Development

## Date: 2025-12-20 to 2025-12-21

### Initial Database Connection
1. can you connect to the MySQL database server running on this computer?
2. try without a password
3. qwu2tnq0BVC@fna3vpf
4. try ipTawIl78?
5. generate scripts to replicate the tp database with all content in mssql server

### MSSQL Database Queries
6. how many projects are in the tp database on the mssql server running in docker?
7. connect to the mssql database at localhos:1433 with sa and password karst-host-&01 and tell me how many projects are in the tp database
8. try with sqlcmd
9. try with sqlcmd is was recently installed

### Monday.com Integration
10. change you directory to ~/Development/scheduler
11. use the monday.txt files to connnect to monday.com retrive the details of the projects in the listed groups. use the project details from monday.com to update the projects table in the projects table in the tp database on the mssql server at localhost,1433. the connection details for the mssql server are in the tp-database.txt file.
12. there should be 65 projects in "In Progres/Scheduled" and 126 projects in "Backlog" make an adjustment to the retrieval from monday.com to get all the projects
13. based on the current sync_monday_to_mssql.py script, create a new script to update the assignments table for developers. Match the the prodjects to developers and use the project's "Developer Timeline" for the start and end dates.
14. list all the projects on the monday.com mms board where the developer is an unknown developer in the mssql tp database

### Spring Boot + Angular Application Setup
15. act as an expert full stack web developer
16. I want to build a springboot app with an angular UI, what are the first steps/
17. Guide me to install Java 17/21 first
18. Java 21 installed, continue
19. JAVA_HOME and PATH already set, contiue
20. Angular CLI installed
21. the last step appeared to be stuck, continue from the last completed step
22. the api is not running how do I start it?
23. yes create the Angular frontend
24. The app is up

### Building the Project Scheduler UI
25. the update did not work. getting error [ERROR] TS2304: Cannot find name 'Assignment'. [plugin angular-compiler] from the server
26. update the "Project Scheduler" page to show a list of the project in a grid. The grid should have the following elements.
27. update the "Project Scheduler" page to show a list of the project in a grid. The grid should have the following elements. {project name, stats, level, target prod date, primnary team name, priority, urgency, dev hours, wf hours, assigned developers)

### Database Schema Updates
28. update the tp database at localhost,1433 to add start_date(date) and end_date(date) to the projects table
29. remove the end_date just added from the spring boot entity model

### Grid Enhancements
30. update the grid on the "Project Scheduler" page to add "Start Date" and "QA Ready Date" to the grid between "Level" and "Target Prod Date"
31. make the grid on the "Project Scehduler" page sortable
32. show me the changes neeed to update the grid on the "Project Scheduler" to show the assignment start and end dates for each developer. do not implement.

### Version Control
33. before implementing push this project to gitHub

### Assignment Date Editing
34. implement "Option 1: Stacked Layout" for developer dates
35. what are the options for allowing the develope's assignment dates to be changed?
36. yes
37. saving the assignment is giving an error ":8080/api/assignments" Failed to load resource
38. the values save, but the card remains open with the button saying "Saving", close the card after successful update

### Developer Card Enhancements
39. add the deveveloper's positon to the developer card after the developer's name in brackets. Use DEV for developer and LEAD for "Technical Lead"
40. the project grid does not load unless the statsus filter is chosen
41. add the ratio value to the developer's card after the positon value, display as a percentage and allow the value to be changed in incraments of 10%. update the assignments table if changed. save as a decimal with 1 representing 100%.

### UI Styling
42. update the developer card make the position display font values the same as the developer name, make the ratio display the same size as the name
43. update all font colors to be 30% darker

### Documentation
44. create a markdown file with all the prompts from this session

---

## Summary

This session covered:
- MySQL and MSSQL database connections and queries
- Monday.com API integration for project synchronization
- Full-stack development with Spring Boot (Java 21) and Angular
- Building a Project Scheduler application with:
  - Project grid with sortable columns
  - Developer assignment management
  - Inline editing for assignment dates and ratios
  - Status filtering and search functionality
- Database schema updates
- UI/UX improvements including styling and color adjustments
- Version control with Git and GitHub

## Technologies Used
- **Backend**: Spring Boot, Java 21, Maven, MSSQL Server
- **Frontend**: Angular, TypeScript, SCSS
- **Database**: MySQL, MSSQL (Azure SQL Edge in Docker)
- **Integration**: Monday.com API, Python scripts
- **Tools**: sqlcmd, Git, npm, Homebrew
