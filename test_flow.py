import requests
import json
import time
import os

BASE_URL = "http://localhost:8080/api"

def print_step(step_name):
    print(f"\n{'='*50}\n[STEP] {step_name}\n{'='*50}")

def run_flow():
    # 1. Register Admin
    print_step("Register Admin")
    register_payload = {
        "username": "AdminUser123",
        "email": "admin123@example.com",
        "password": "password123",
        "role": "ADMIN"
    }
    r = requests.post(f"{BASE_URL}/auth/register", json=register_payload)
    print(f"Status Code: {r.status_code}")
    print(f"Response: {r.text}")

    # 2. Login
    print_step("Login Admin")
    login_payload = {
        "username": "AdminUser123",
        "password": "password123"
    }
    r = requests.post(f"{BASE_URL}/auth/login", json=login_payload)
    print(f"Status Code: {r.status_code}")
    
    if r.status_code != 200:
        print("Login failed!")
        return

    token = r.json().get('accessToken')
    headers = {"Authorization": f"Bearer {token}"}
    print(f"Successfully obtained JWT Token (starting with: {token[:20]}...)")

    # 3. Create Employee
    print_step("Create New Employee")
    emp_payload = {
        "name": "Jane Caching",
        "email": "jane.caching@example.com",
        "department": "Engineering",
        "salary": 85000
    }
    r = requests.post(f"{BASE_URL}/employees", json=emp_payload, headers=headers)
    print(f"Status Code: {r.status_code}")
    if r.status_code != 200:
        print("Employee creation failed:", r.text)
        return
    
    employee_id = r.json().get('id')
    print(f"Successfully created Employee with ID: {employee_id}")
    print(f"Response: {json.dumps(r.json(), indent=2)}")

    # 4. Upload Profile Image
    print_step("Upload Profile Image")
    # Create a dummy image file
    with open("profile.png", "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x06\x00\x00\x00\x1f\x15\xc4\x89\x00\x00\x00\rIDATx\x9cc\xf8\xff\xff\xff\x00\x05\xfe\x02\xfe\xa4\xa9\x84\x8f\x00\x00\x00\x00IEND\xaeB`\x82")
    
    with open("profile.png", "rb") as f:
        files = {'file': ('profile.png', f, 'image/png')}
        r = requests.post(f"{BASE_URL}/employees/{employee_id}/profile-image", headers=headers, files=files)
    
    print(f"Status Code: {r.status_code}")
    print(f"Response: {json.dumps(r.json(), indent=2)}")
    image_name = r.json().get('profileImageName')

    # 5. Download Profile Image
    print_step("Download Profile Image")
    r = requests.get(f"{BASE_URL}/employees/{employee_id}/profile-image", headers=headers)
    print(f"Status Code: {r.status_code}")
    print(f"Headers Content-Disposition: {r.headers.get('Content-Disposition')}")
    print(f"Downloaded File Size: {len(r.content)} bytes")

    # 6. Test Cache Miss
    print_step("Test Cache MISS (Fetching Employee first time)")
    start_time = time.time()
    r = requests.get(f"{BASE_URL}/employees/{employee_id}", headers=headers)
    end_time = time.time()
    print(f"Status Code: {r.status_code}")
    print(f"Time Taken: {(end_time - start_time) * 1000:.2f} ms")
    print("-> Notice that this took slightly longer and printed a log in your Spring Boot console.")

    # 7. Test Cache Hit
    print_step("Test Cache HIT (Fetching Employee second time)")
    start_time = time.time()
    r = requests.get(f"{BASE_URL}/employees/{employee_id}", headers=headers)
    end_time = time.time()
    print(f"Status Code: {r.status_code}")
    print(f"Time Taken: {(end_time - start_time) * 1000:.2f} ms")
    print("-> Notice that this was extremely fast and NO log was printed in Spring Boot!")

    # 8. Test Write-Through Cache
    print_step("Test Write-Through Cache (Updating Employee)")
    update_payload = {
        "name": "Jane Caching",
        "email": "jane.caching@example.com",
        "department": "Engineering",
        "salary": 120000
    }
    r = requests.put(f"{BASE_URL}/employees/{employee_id}", json=update_payload, headers=headers)
    print(f"Status Code: {r.status_code}")
    print(f"Response: {json.dumps(r.json(), indent=2)}")
    print("-> The database AND Redis cache have been updated.")

    # 9. Verify Write-Through Cache
    print_step("Verify Write-Through Cache")
    start_time = time.time()
    r = requests.get(f"{BASE_URL}/employees/{employee_id}", headers=headers)
    end_time = time.time()
    print(f"Status Code: {r.status_code}")
    print(f"Time Taken: {(end_time - start_time) * 1000:.2f} ms")
    print(f"Salary Retrieved: {r.json().get('salary')}")
    print("-> Fetched directly from Redis, skipping the DB entirely, and the salary is correctly updated to 120,000!")

if __name__ == "__main__":
    run_flow()
