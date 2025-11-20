#!/usr/bin/env python3
import xml.etree.ElementTree as ET
import sys
import os

def extract_mutation_score(xml_file):
    """Extract mutation coverage score from PIT XML report"""
    try:
        tree = ET.parse(xml_file)
        root = tree.getroot()
        
        # PIT XML structure: <mutations><mutation detected="true|false">
        total_mutations = 0
        killed_mutations = 0
        
        for mutation in root.findall('.//mutation'):
            total_mutations += 1
            if mutation.get('detected') == 'true':
                killed_mutations += 1
        
        if total_mutations == 0:
            print("❌ No mutations found in report!")
            return 0
        
        score = (killed_mutations / total_mutations) * 100
        print(f"📊 Mutation Score: {score:.1f}% ({killed_mutations}/{total_mutations} killed)")
        return score
    
    except Exception as e:
        print(f"❌ Error parsing XML: {e}")
        sys.exit(1)

def read_baseline(baseline_file):
    """Read baseline score from file"""
    try:
        with open(baseline_file, 'r') as f:
            baseline = float(f.read().strip())
            print(f"📌 Baseline Score: {baseline:.1f}%")
            return baseline
    except FileNotFoundError:
        print("⚠️  No baseline found, using 0%")
        return 0.0
    except Exception as e:
        print(f"❌ Error reading baseline: {e}")
        sys.exit(1)

def main():
    xml_report = "core/target/pit-reports/mutations.xml"
    baseline_file = ".github/mutation-baseline.txt"
    
    if not os.path.exists(xml_report):
        print(f"❌ PIT report not found: {xml_report}")
        sys.exit(1)
    
    current_score = extract_mutation_score(xml_report)
    baseline_score = read_baseline(baseline_file)
    
    print(f"\n{'='*50}")
    if current_score >= baseline_score:
        print(f"✅ PASS: Score {current_score:.1f}% >= Baseline {baseline_score:.1f}%")
        print(f"{'='*50}")
        sys.exit(0)
    else:
        print(f"❌ FAIL: Score {current_score:.1f}% < Baseline {baseline_score:.1f}%")
        print(f"   Score dropped by {baseline_score - current_score:.1f}%!")
        print(f"{'='*50}")
        sys.exit(1)

if __name__ == "__main__":
    main()
