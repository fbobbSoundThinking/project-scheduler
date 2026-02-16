  File "/Users/frankbobb/Development/scheduler/generate_team_update_sql.py", line 67
    print(f"-- UPDATE projects SET primary_team_id = (SELECT TOP 1 team_id FROM teams WHERE team_name = '{team_name.replace(\"'\", \"''\")}') WHERE project_name = '{escaped_name}';")
                                                                                                                                                                                     ^
SyntaxError: f-string expression part cannot include a backslash
